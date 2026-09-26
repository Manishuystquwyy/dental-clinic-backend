package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.dto.request.PaymentRequestDto;
import com.gayatri.dentalclinic.entity.Appointment;
import com.gayatri.dentalclinic.entity.Bill;
import com.gayatri.dentalclinic.entity.Payment;
import com.gayatri.dentalclinic.enums.AppointmentStatus;
import com.gayatri.dentalclinic.enums.PaymentMode;
import com.gayatri.dentalclinic.enums.PaymentStatus;
import com.gayatri.dentalclinic.enums.Role;
import com.gayatri.dentalclinic.repository.BillRepository;
import com.gayatri.dentalclinic.repository.PaymentRepository;
import com.gayatri.dentalclinic.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PaymentServiceImplTest {
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final BillRepository bills = mock(BillRepository.class);
    private final PaymentServiceImpl service = new PaymentServiceImpl(payments, bills);

    @BeforeEach
    void setUp() {
        var admin = new CustomUserDetails(1L, "admin@example.com", "hash", Role.ADMIN, null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities()));
        when(payments.save(any(Payment.class))).thenAnswer(call -> call.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @EnumSource(AppointmentStatus.class)
    void recordingSuccessfulPaymentDoesNotChangeAppointmentStatus(AppointmentStatus status) {
        var appointment = Appointment.builder().id(25L).status(status).build();
        var bill = Bill.builder().id(5L).appointment(appointment).finalAmount(BigDecimal.TEN).build();
        when(bills.findById(5L)).thenReturn(Optional.of(bill));

        assertEquals(PaymentStatus.SUCCESS, service.createPayment(successfulPayment()).getStatus());
        assertEquals(status, appointment.getStatus());
    }

    @ParameterizedTest
    @EnumSource(AppointmentStatus.class)
    void updatingPaymentToSuccessDoesNotChangeAppointmentStatus(AppointmentStatus status) {
        var appointment = Appointment.builder().id(25L).status(status).build();
        var bill = Bill.builder().id(5L).appointment(appointment).finalAmount(BigDecimal.TEN).build();
        var payment = Payment.builder().id(9L).bill(bill).status(PaymentStatus.PENDING).build();
        when(bills.findById(5L)).thenReturn(Optional.of(bill));
        when(payments.findById(9L)).thenReturn(Optional.of(payment));

        assertEquals(PaymentStatus.SUCCESS, service.updatePayment(9L, successfulPayment()).getStatus());
        assertEquals(status, appointment.getStatus());
    }

    private PaymentRequestDto successfulPayment() {
        return new PaymentRequestDto(5L, PaymentMode.CASH, BigDecimal.TEN,
                LocalDate.of(2026, 9, 24), PaymentStatus.SUCCESS);
    }
}
