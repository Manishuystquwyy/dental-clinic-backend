package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.dto.request.RazorpayOrderRequestDto;
import com.gayatri.dentalclinic.dto.request.RazorpayVerificationRequestDto;
import com.gayatri.dentalclinic.entity.*;
import com.gayatri.dentalclinic.enums.Role;
import com.gayatri.dentalclinic.repository.*;
import com.gayatri.dentalclinic.security.CustomUserDetails;
import com.gayatri.dentalclinic.service.BookingTime;
import com.gayatri.dentalclinic.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RazorpayTimezoneTest {
    private final DentistRepository dentists = mock(DentistRepository.class);
    private final PatientRepository patients = mock(PatientRepository.class);
    private final AppointmentRepository appointments = mock(AppointmentRepository.class);
    private final BillRepository bills = mock(BillRepository.class);
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final RazorpayCheckoutSessionRepository sessions = mock(RazorpayCheckoutSessionRepository.class);
    private final Dentist dentist = Dentist.builder().id(10L).name("Doctor").consultationFees(BigDecimal.ONE).build();
    private final Patient patient = Patient.builder().id(3L).firstName("Test").lastName("Patient").build();
    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://api.razorpay.com/v1");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    private RazorpayPaymentServiceImpl service(String now) {
        var service = new RazorpayPaymentServiceImpl(dentists, patients, appointments, bills, payments, sessions,
                mock(NotificationService.class), new BookingTime(Clock.fixed(Instant.parse(now), ZoneOffset.UTC)), builder.build());
        ReflectionTestUtils.setField(service, "razorpayKeyId", "test");
        ReflectionTestUtils.setField(service, "razorpayKeySecret", "test-secret");
        ReflectionTestUtils.setField(service, "razorpayWebhookSecret", "test-secret");
        ReflectionTestUtils.setField(service, "currency", "INR");
        var user = new CustomUserDetails(2L, "patient@example.com", "hash", Role.PATIENT, 3L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        return service;
    }
    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test
    void checkoutTimestampsUseIstAfterIndianMidnightOnUtcHost() {
        when(patients.findById(3L)).thenReturn(Optional.of(patient));
        when(dentists.findById(10L)).thenReturn(Optional.of(dentist));
        server.expect(requestTo("https://api.razorpay.com/v1/orders"))
                .andRespond(withSuccess("{\"id\":\"order_test\",\"amount\":100,\"currency\":\"INR\"}", MediaType.APPLICATION_JSON));
        service("2026-09-24T19:59:30Z").createOrder(new RazorpayOrderRequestDto(10L,LocalDate.of(2026,9,25),LocalTime.of(10,30),null));
        var capture=org.mockito.ArgumentCaptor.forClass(RazorpayCheckoutSession.class);
        verify(sessions).save(capture.capture());
        assertEquals(LocalDateTime.of(2026,9,25,1,29,30),capture.getValue().getCreatedAt());
        assertEquals(capture.getValue().getCreatedAt(),capture.getValue().getUpdatedAt());
        server.verify();
    }

    private RazorpayCheckoutSession prepareSession() {
        var session=RazorpayCheckoutSession.builder().id(1L).patient(patient).dentist(dentist)
                .appointmentDate(LocalDate.of(2026,9,28)).appointmentTime(LocalTime.of(10,30))
                .razorpayOrderId("order_test").amount(BigDecimal.ONE).status(RazorpayCheckoutStatus.CREATED).build();
        when(sessions.findWithLockByRazorpayOrderId("order_test")).thenReturn(Optional.of(session));
        when(dentists.findWithLockById(10L)).thenReturn(Optional.of(dentist));
        when(appointments.save(any())).thenAnswer(i -> { Appointment a=i.getArgument(0);a.setId(9L);return a; });
        when(bills.save(any())).thenAnswer(i -> { Bill b=i.getArgument(0);b.setId(8L);return b; });
        return session;
    }
    private String entity() {
        return "{\"id\":\"pay_test\",\"order_id\":\"order_test\",\"amount\":100,\"status\":\"captured\",\"method\":\"upi\",\"created_at\":"
                +Instant.parse("2026-09-24T19:59:53Z").getEpochSecond()+"}";
    }
    private String sign(String value) throws Exception {
        var mac=Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-secret".getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
    private void assertDates(RazorpayCheckoutSession session, LocalDateTime expectedUpdate) {
        var pc=org.mockito.ArgumentCaptor.forClass(Payment.class);verify(payments).save(pc.capture());
        assertEquals(LocalDate.of(2026,9,25),pc.getValue().getPaymentDate());
        var bc=org.mockito.ArgumentCaptor.forClass(Bill.class);verify(bills).save(bc.capture());
        assertEquals(expectedUpdate.toLocalDate(),bc.getValue().getBillDate());
        assertEquals(expectedUpdate,session.getUpdatedAt());
    }
    @Test
    void browserVerificationUsesGatewayDateAndClinicUpdateTime() throws Exception {
        var session=prepareSession();
        server.expect(requestTo("https://api.razorpay.com/v1/payments/pay_test")).andRespond(withSuccess(entity(),MediaType.APPLICATION_JSON));
        var dto=new RazorpayVerificationRequestDto();
        dto.setDentistId(10L);dto.setAppointmentDate(session.getAppointmentDate());dto.setAppointmentTime(session.getAppointmentTime());
        dto.setRazorpayOrderId("order_test");dto.setRazorpayPaymentId("pay_test");dto.setRazorpaySignature(sign("order_test|pay_test"));
        service("2026-09-24T20:00:00Z").verifyPaymentAndConfirmAppointment(dto);
        assertDates(session,LocalDateTime.of(2026,9,25,1,30));server.verify();
    }
    @Test
    void delayedWebhookRetainsActualPaymentDateInsteadOfDeliveryDate() throws Exception {
        var session=prepareSession();
        String payload="{\"event\":\"payment.captured\",\"payload\":{\"payment\":{\"entity\":"+entity()+"}}}";
        service("2026-09-26T19:00:00Z").handleWebhook(payload,sign(payload));
        assertDates(session,LocalDateTime.of(2026,9,27,0,30));
    }
}
