package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.dto.request.RazorpayOrderRequestDto;
import com.gayatri.dentalclinic.dto.request.RazorpayVerificationRequestDto;
import com.gayatri.dentalclinic.dto.response.AppointmentResponseDto;
import com.gayatri.dentalclinic.dto.response.RazorpayOrderResponseDto;
import com.gayatri.dentalclinic.entity.Appointment;
import com.gayatri.dentalclinic.entity.Bill;
import com.gayatri.dentalclinic.entity.Dentist;
import com.gayatri.dentalclinic.entity.Patient;
import com.gayatri.dentalclinic.entity.Payment;
import com.gayatri.dentalclinic.entity.RazorpayCheckoutSession;
import com.gayatri.dentalclinic.entity.RazorpayCheckoutStatus;
import com.gayatri.dentalclinic.enums.AppointmentStatus;
import com.gayatri.dentalclinic.enums.PaymentMode;
import com.gayatri.dentalclinic.enums.PaymentStatus;
import com.gayatri.dentalclinic.enums.Role;
import com.gayatri.dentalclinic.exception.BadRequestException;
import com.gayatri.dentalclinic.exception.NotFoundException;
import com.gayatri.dentalclinic.mapper.AppointmentMapper;
import com.gayatri.dentalclinic.repository.AppointmentRepository;
import com.gayatri.dentalclinic.repository.BillRepository;
import com.gayatri.dentalclinic.repository.DentistRepository;
import com.gayatri.dentalclinic.repository.PatientRepository;
import com.gayatri.dentalclinic.repository.PaymentRepository;
import com.gayatri.dentalclinic.repository.RazorpayCheckoutSessionRepository;
import com.gayatri.dentalclinic.security.SecurityUtils;
import com.gayatri.dentalclinic.service.NotificationService;
import com.gayatri.dentalclinic.service.BookingTime;
import com.gayatri.dentalclinic.service.RazorpayPaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class RazorpayPaymentServiceImpl implements RazorpayPaymentService {

    private static final List<LocalTime> BOOKING_SLOTS = List.of(
            LocalTime.of(10, 30), LocalTime.of(11, 0), LocalTime.of(14, 0),
            LocalTime.of(14, 30), LocalTime.of(15, 30), LocalTime.of(16, 0),
            LocalTime.of(16, 30), LocalTime.of(17, 0), LocalTime.of(17, 30),
            LocalTime.of(18, 0));

    private final DentistRepository dentistRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final RazorpayCheckoutSessionRepository checkoutSessionRepository;
    private final NotificationService notificationService;
    private final BookingTime bookingTime;
    private final RestClient razorpayRestClient;
    private final JsonParser jsonParser = JsonParserFactory.getJsonParser();

    private final String razorpayKeyId;
    private final String razorpayKeySecret;
    private final String currency;
    private final String razorpayWebhookSecret;

    public RazorpayPaymentServiceImpl(DentistRepository dentistRepository,
            PatientRepository patientRepository,
            AppointmentRepository appointmentRepository,
            BillRepository billRepository,
            PaymentRepository paymentRepository,
            RazorpayCheckoutSessionRepository checkoutSessionRepository,
            NotificationService notificationService,
            BookingTime bookingTime,
            RestClient razorpayRestClient,
            @Value("${app.razorpay.key-id:}") String razorpayKeyId,
            @Value("${app.razorpay.key-secret:}") String razorpayKeySecret,
            @Value("${app.razorpay.currency:INR}") String currency,
            @Value("${app.razorpay.webhook-secret:}") String razorpayWebhookSecret) {
        this.dentistRepository = dentistRepository;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.billRepository = billRepository;
        this.paymentRepository = paymentRepository;
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.notificationService = notificationService;
        this.bookingTime = bookingTime;
        this.razorpayRestClient = razorpayRestClient;
        this.razorpayKeyId = razorpayKeyId;
        this.razorpayKeySecret = razorpayKeySecret;
        this.currency = currency;
        this.razorpayWebhookSecret = razorpayWebhookSecret;
    }

    @Override
    public RazorpayOrderResponseDto createOrder(RazorpayOrderRequestDto requestDto) {
        Long patientId = requirePatient();
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new NotFoundException("Patient not found with id: " + patientId));
        Dentist dentist = findDentist(requestDto.getDentistId());
        ensureSlotIsAvailable(dentist.getId(), requestDto.getAppointmentDate(), requestDto.getAppointmentTime());
        BigDecimal fee = consultationFee(dentist);

        String receipt = buildReceipt(patient.getId(), dentist.getId(), requestDto.getAppointmentDate(), requestDto.getAppointmentTime());
        Map<String, Object> payload = Map.of(
                "amount", toPaise(fee),
                "currency", currency,
                "receipt", receipt,
                "notes", Map.of(
                        "patientId", String.valueOf(patient.getId()),
                        "dentistId", String.valueOf(dentist.getId()),
                        "appointmentDate", String.valueOf(requestDto.getAppointmentDate()),
                        "appointmentTime", String.valueOf(requestDto.getAppointmentTime())
                )
        );

        try {
            Map<?, ?> order = razorpayClient().post()
                    .uri("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(Map.class);

            String orderId = getText(order, "id");
            if (orderId.isBlank()) {
                throw new BadRequestException("Unable to create Razorpay order.");
            }

            persistCheckoutSession(patient, dentist, requestDto, fee, orderId);

            return RazorpayOrderResponseDto.builder()
                    .keyId(razorpayKeyId)
                    .orderId(orderId)
                    .amount(getLong(order, "amount"))
                    .currency(getText(order, "currency", currency))
                    .name("Gayatri Dental Clinic")
                    .description("Consultation booking with " + dentist.getName())
                    .build();
        } catch (RestClientException ex) {
            log.error("Failed to create Razorpay order", ex);
            throw new BadRequestException("Unable to create Razorpay order. Please try again.");
        }
    }

    @Override
    @Transactional
    public AppointmentResponseDto verifyPaymentAndConfirmAppointment(RazorpayVerificationRequestDto requestDto) {
        Long patientId = requirePatient();
        verifySignature(requestDto.getRazorpayOrderId(), requestDto.getRazorpayPaymentId(), requestDto.getRazorpaySignature());
        Map<?, ?> paymentNode = fetchPayment(requestDto.getRazorpayPaymentId());
        RazorpayCheckoutSession session = getLockedSession(requestDto.getRazorpayOrderId());
        if (!session.getPatient().getId().equals(patientId)) {
            throw new AccessDeniedException("This Razorpay order does not belong to the current patient.");
        }
        validateVerificationRequestMatchesSession(session, requestDto);
        validateVerifiedPayment(paymentNode, requestDto, session.getAmount());

        return finalizeSuccessfulPayment(
                session,
                requestDto.getRazorpayPaymentId(),
                requestDto.getRazorpaySignature(),
                getText(paymentNode, "method"),
                paymentDate(paymentNode)
        );
    }

    @Override
    @Transactional
    public void handleWebhook(String rawPayload, String signature) {
        verifyWebhookSignature(rawPayload, signature);

        Map<String, Object> payload = jsonParser.parseMap(rawPayload);
        String event = getText(payload, "event");

        if (!"payment.captured".equalsIgnoreCase(event)) {
            return;
        }

        Map<?, ?> paymentEntity = getNestedMap(payload, "payload", "payment", "entity");
        String orderId = getText(paymentEntity, "order_id");
        String paymentId = getText(paymentEntity, "id");

        if (orderId.isBlank() || paymentId.isBlank()) {
            throw new BadRequestException("Invalid Razorpay webhook payload.");
        }

        RazorpayCheckoutSession session = getLockedSession(orderId);
        validateWebhookPayment(session, paymentEntity);

        finalizeSuccessfulPayment(
                session,
                paymentId,
                signature,
                getText(paymentEntity, "method"),
                paymentDate(paymentEntity)
        );
    }

    private Long requirePatient() {
        Long patientId = SecurityUtils.getCurrentPatientId();
        if (SecurityUtils.getCurrentRole() != Role.PATIENT || patientId == null) {
            throw new AccessDeniedException("Only logged-in patients can complete Razorpay payments.");
        }
        return patientId;
    }

    private Dentist findDentist(Long dentistId) {
        return dentistRepository.findById(dentistId)
                .orElseThrow(() -> new NotFoundException("Dentist not found with id: " + dentistId));
    }

    private Dentist findLockedDentist(Long dentistId) {
        return dentistRepository.findWithLockById(dentistId)
                .orElseThrow(() -> new NotFoundException("Dentist not found with id: " + dentistId));
    }

    private BigDecimal consultationFee(Dentist dentist) {
        return dentist.getConsultationFees() != null ? dentist.getConsultationFees() : BigDecimal.ZERO;
    }

    private long toPaise(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).longValueExact();
    }

    private String buildReceipt(Long patientId, Long dentistId, LocalDate date, LocalTime time) {
        return ("p" + patientId + "-d" + dentistId + "-" + date + "-" + time).replace(":", "");
    }

    private RestClient razorpayClient() {
        ensureGatewayConfigured();
        return razorpayRestClient;
    }

    private void verifySignature(String orderId, String paymentId, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String payload = orderId + "|" + paymentId;
            String expected = bytesToHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            if (!expected.equals(signature)) {
                throw new BadRequestException("Razorpay signature verification failed.");
            }
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Unable to verify Razorpay payment signature.");
        }
    }

    private void verifyWebhookSignature(String rawPayload, String signature) {
        if (razorpayWebhookSecret == null || razorpayWebhookSecret.isBlank()) {
            throw new BadRequestException("Razorpay webhook secret is not configured on the server.");
        }
        String expected = hmacSha256(rawPayload, razorpayWebhookSecret);
        if (!expected.equals(signature)) {
            throw new BadRequestException("Razorpay webhook signature verification failed.");
        }
    }

    private Map<?, ?> fetchPayment(String paymentId) {
        // Only retry a read. Retrying order creation could create another checkout.
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return fetchPaymentOnce(paymentId);
            } catch (ResourceAccessException ex) {
                if (attempt == 0) {
                    log.warn("Razorpay payment lookup connection failed; retrying once");
                    continue;
                }
                log.error("Failed to fetch Razorpay payment {}", paymentId, ex);
            } catch (RestClientException ex) {
                log.error("Failed to fetch Razorpay payment {}", paymentId, ex);
                break;
            }
        }
        throw new BadRequestException("Payment verification is temporarily unavailable. "
                + "If money was deducted, do not pay again. Please contact the clinic with your payment ID.");
    }

    private Map<?, ?> fetchPaymentOnce(String paymentId) {
        Map<?, ?> payment = razorpayClient().get()
                .uri("/payments/{id}", paymentId)
                .retrieve()
                .body(Map.class);
        if (getText(payment, "id").isBlank()) {
            throw new BadRequestException("Unable to verify Razorpay payment.");
        }
        return payment;
    }

    private void validateVerifiedPayment(Map<?, ?> paymentNode, RazorpayVerificationRequestDto requestDto, BigDecimal fee) {
        String orderId = getText(paymentNode, "order_id");
        if (!requestDto.getRazorpayOrderId().equals(orderId)) {
            throw new BadRequestException("Razorpay order mismatch.");
        }

        long amount = getLong(paymentNode, "amount");
        if (amount != toPaise(fee)) {
            throw new BadRequestException("Razorpay payment amount does not match consultation fee.");
        }

        String status = getText(paymentNode, "status");
        if (!"captured".equalsIgnoreCase(status) && !"authorized".equalsIgnoreCase(status)) {
            throw new BadRequestException("Razorpay payment is not successful.");
        }
    }

    private PaymentMode mapPaymentMode(String method) {
        return switch (method == null ? "" : method.toLowerCase()) {
            case "upi" -> PaymentMode.UPI;
            case "card" -> PaymentMode.CARD;
            case "netbanking" -> PaymentMode.NET_BANKING;
            default -> PaymentMode.CASH;
        };
    }

    private LocalDate paymentDate(Map<?, ?> payment) {
        long createdAt = getLong(payment, "created_at");
        if (createdAt <= 0) {
            throw new BadRequestException("Razorpay payment timestamp is missing or invalid.");
        }
        return bookingTime.dateAt(Instant.ofEpochSecond(createdAt));
    }

    private void ensureGatewayConfigured() {
        if (razorpayKeyId == null || razorpayKeyId.isBlank()
                || razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            throw new BadRequestException("Razorpay is not configured on the server.");
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private String hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return bytesToHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new BadRequestException("Unable to compute Razorpay signature.");
        }
    }

    private String getText(Map<?, ?> source, String key) {
        return getText(source, key, "");
    }

    private String getText(Map<?, ?> source, String key, String fallback) {
        if (source == null) {
            return fallback;
        }
        Object value = source.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private long getLong(Map<?, ?> source, String key) {
        if (source == null) {
            return 0L;
        }
        Object value = source.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text);
        }
        return 0L;
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> getNestedMap(Map<?, ?> source, String... keys) {
        Object current = source;
        for (String key : keys) {
            if (!(current instanceof Map<?, ?> currentMap)) {
                return Map.of();
            }
            current = currentMap.get(key);
        }
        return current instanceof Map<?, ?> nested ? nested : Map.of();
    }

    private void persistCheckoutSession(
            Patient patient,
            Dentist dentist,
            RazorpayOrderRequestDto requestDto,
            BigDecimal fee,
            String orderId
    ) {
        LocalDateTime now = bookingTime.now();
        RazorpayCheckoutSession session = RazorpayCheckoutSession.builder()
                .patient(patient)
                .dentist(dentist)
                .appointmentDate(requestDto.getAppointmentDate())
                .appointmentTime(requestDto.getAppointmentTime())
                .remarks(requestDto.getRemarks())
                .amount(fee)
                .razorpayOrderId(orderId)
                .status(RazorpayCheckoutStatus.CREATED)
                .createdAt(now)
                .updatedAt(now)
                .build();
        checkoutSessionRepository.save(session);
    }

    private RazorpayCheckoutSession getLockedSession(String orderId) {
        return checkoutSessionRepository.findWithLockByRazorpayOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("Razorpay checkout session not found for order: " + orderId));
    }

    private void validateVerificationRequestMatchesSession(
            RazorpayCheckoutSession session,
            RazorpayVerificationRequestDto requestDto
    ) {
        if (!session.getDentist().getId().equals(requestDto.getDentistId())
                || !session.getAppointmentDate().equals(requestDto.getAppointmentDate())
                || !session.getAppointmentTime().equals(requestDto.getAppointmentTime())
                || !safeEquals(session.getRemarks(), requestDto.getRemarks())) {
            throw new BadRequestException("Razorpay verification does not match the original booking details.");
        }
    }

    private void validateWebhookPayment(RazorpayCheckoutSession session, Map<?, ?> paymentEntity) {
        long amount = getLong(paymentEntity, "amount");
        if (amount != toPaise(session.getAmount())) {
            throw new BadRequestException("Webhook payment amount does not match consultation fee.");
        }

        String status = getText(paymentEntity, "status");
        if (!"captured".equalsIgnoreCase(status)) {
            throw new BadRequestException("Webhook payment is not captured.");
        }
    }

    private AppointmentResponseDto finalizeSuccessfulPayment(
            RazorpayCheckoutSession session,
            String paymentId,
            String signature,
            String method,
            LocalDate paymentDate
    ) {
        if (session.getConfirmedAppointment() != null) {
            if (session.getRazorpayPaymentId() != null && !session.getRazorpayPaymentId().equals(paymentId)) {
                throw new BadRequestException("This Razorpay order is already linked to a different payment.");
            }
            return AppointmentMapper.toDto(session.getConfirmedAppointment());
        }

        if (paymentRepository.existsByGatewayPaymentId(paymentId)) {
            Payment existingPayment = paymentRepository.findByGatewayPaymentId(paymentId)
                    .orElseThrow(() -> new BadRequestException("Payment already processed."));
            session.setConfirmedAppointment(existingPayment.getBill().getAppointment());
            session.setRazorpayPaymentId(paymentId);
            session.setStatus(RazorpayCheckoutStatus.PAID);
            session.setUpdatedAt(bookingTime.now());
            checkoutSessionRepository.save(session);
            return AppointmentMapper.toDto(existingPayment.getBill().getAppointment());
        }

        Dentist dentist = findLockedDentist(session.getDentist().getId());
        ensureSlotIsAvailable(dentist.getId(), session.getAppointmentDate(), session.getAppointmentTime());

        Appointment appointment = Appointment.builder()
                .patient(session.getPatient())
                .dentist(session.getDentist())
                .appointmentDate(session.getAppointmentDate())
                .appointmentTime(session.getAppointmentTime())
                .status(AppointmentStatus.BOOKED)
                .remarks(session.getRemarks())
                .build();
        Appointment savedAppointment = appointmentRepository.save(appointment);

        LocalDate today = bookingTime.now().toLocalDate();
        Bill bill = Bill.builder()
                .appointment(savedAppointment)
                .totalAmount(session.getAmount())
                .discount(BigDecimal.ZERO)
                .finalAmount(session.getAmount())
                .billDate(today)
                .build();
        Bill savedBill = billRepository.save(bill);

        Payment payment = Payment.builder()
                .bill(savedBill)
                .paymentMode(mapPaymentMode(method))
                .amount(session.getAmount())
                .paymentDate(paymentDate)
                .status(PaymentStatus.SUCCESS)
                .gatewayOrderId(session.getRazorpayOrderId())
                .gatewayPaymentId(paymentId)
                .gatewaySignature(signature)
                .build();
        paymentRepository.save(payment);

        session.setRazorpayPaymentId(paymentId);
        session.setStatus(RazorpayCheckoutStatus.PAID);
        session.setConfirmedAppointment(savedAppointment);
        session.setUpdatedAt(bookingTime.now());
        checkoutSessionRepository.save(session);

        try {
            notificationService.sendAppointmentConfirmation(session.getPatient(), session.getDentist(), savedAppointment);
        } catch (Exception ex) {
            log.warn("Failed to send appointment confirmation notification", ex);
        }

        return AppointmentMapper.toDto(savedAppointment);
    }

    private boolean safeEquals(String left, String right) {
        return (left == null ? "" : left).equals(right == null ? "" : right);
    }

    private void ensureSlotIsAvailable(Long dentistId, LocalDate date, LocalTime time) {
        bookingTime.requireFuture(date, time);
        if (!BOOKING_SLOTS.contains(time)) {
            throw new BadRequestException("The selected appointment time is not an available booking slot.");
        }
        boolean alreadyBooked = appointmentRepository
                .existsByDentistIdAndAppointmentDateAndAppointmentTimeAndStatusIn(
                        dentistId, date, time, List.of(AppointmentStatus.BOOKED, AppointmentStatus.COMPLETED));
        if (alreadyBooked) {
            throw new BadRequestException("This dentist is no longer available for the selected date and time slot.");
        }
    }
}
