package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.dto.request.RazorpayOrderRequestDto;
import com.gayatri.dentalclinic.entity.*;
import com.gayatri.dentalclinic.enums.Role;
import com.gayatri.dentalclinic.exception.BadRequestException;
import com.gayatri.dentalclinic.repository.*;
import com.gayatri.dentalclinic.security.CustomUserDetails;
import com.gayatri.dentalclinic.service.BookingTime;
import com.gayatri.dentalclinic.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RazorpayBookingTimeTest {
    private final DentistRepository dentists = mock(DentistRepository.class);
    private final PatientRepository patients = mock(PatientRepository.class);
    private final AppointmentRepository appointments = mock(AppointmentRepository.class);
    private final BillRepository bills = mock(BillRepository.class);
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final RazorpayCheckoutSessionRepository sessions = mock(RazorpayCheckoutSessionRepository.class);
    private final Dentist dentist = Dentist.builder().id(10L).build();
    private RazorpayPaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RazorpayPaymentServiceImpl(dentists, patients, appointments, bills, payments,
                sessions, mock(NotificationService.class),
                new BookingTime(Clock.fixed(Instant.parse("2026-09-24T08:30:00Z"), ZoneOffset.UTC)),
                mock(org.springframework.web.client.RestClient.class));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void expiredSlotsCannotStartPaymentCheckout() {
        var user = new CustomUserDetails(2L, "patient@example.com", "hash", Role.PATIENT, 3L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        when(patients.findById(3L)).thenReturn(Optional.of(Patient.builder().id(3L).build()));
        when(dentists.findById(10L)).thenReturn(Optional.of(dentist));
        for (var dateTime : new LocalDateTime[] {
                LocalDateTime.of(2026, 9, 23, 18, 0),
                LocalDateTime.of(2026, 9, 24, 11, 0),
                LocalDateTime.of(2026, 9, 24, 14, 0)}) {
            var error = assertThrows(BadRequestException.class, () -> service.createOrder(
                    new RazorpayOrderRequestDto(10L, dateTime.toLocalDate(), dateTime.toLocalTime(), null)));
            assertEquals("Please choose a future appointment date and time.", error.getMessage());
        }
        verifyNoInteractions(appointments, bills, payments, sessions);
    }

    @Test
    void paymentCompletionCannotCreateAnAppointmentAfterTheSlotHasStarted() throws Exception {
        ReflectionTestUtils.setField(service, "razorpayWebhookSecret", "test-secret");
        var session = RazorpayCheckoutSession.builder().dentist(dentist)
                .appointmentDate(LocalDate.of(2026, 9, 24)).appointmentTime(LocalTime.of(14, 0))
                .amount(BigDecimal.valueOf(100)).build();
        when(sessions.findWithLockByRazorpayOrderId("order_test")).thenReturn(Optional.of(session));
        when(dentists.findWithLockById(10L)).thenReturn(Optional.of(dentist));
        String payload = """
                {"event":"payment.captured","payload":{"payment":{"entity":{
                "order_id":"order_test","id":"pay_test","amount":10000,"status":"captured","created_at":1790238600}}}}
                """;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        var error = assertThrows(BadRequestException.class, () -> service.handleWebhook(payload, signature));
        assertEquals("Please choose a future appointment date and time.", error.getMessage());
        verifyNoInteractions(appointments, bills);
        verify(payments, never()).save(any());
    }
}
