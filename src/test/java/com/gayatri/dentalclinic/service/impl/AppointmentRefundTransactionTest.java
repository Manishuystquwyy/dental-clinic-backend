package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.dto.request.AppointmentRequestDto;
import com.gayatri.dentalclinic.dto.request.BillRequestDto;
import com.gayatri.dentalclinic.dto.request.PaymentRequestDto;
import com.gayatri.dentalclinic.entity.*;
import com.gayatri.dentalclinic.enums.*;
import com.gayatri.dentalclinic.exception.BadRequestException;
import com.gayatri.dentalclinic.repository.*;
import com.gayatri.dentalclinic.security.CustomUserDetails;
import com.gayatri.dentalclinic.service.AppointmentService;
import com.gayatri.dentalclinic.service.NotificationService;
import com.gayatri.dentalclinic.service.BillService;
import com.gayatri.dentalclinic.service.PaymentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:appointment-refunds;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false", "spring.jpa.open-in-view=false", "app.admin.email=", "app.admin.password=",
        "app.refunds.enabled=false", "app.refunds.worker-enabled=false"
})
class AppointmentRefundTransactionTest {
    @Autowired AppointmentService service;
    @Autowired BillService billService;
    @Autowired PaymentService paymentService;
    @Autowired AppointmentRepository appointments;
    @Autowired PatientRepository patients;
    @Autowired DentistRepository dentists;
    @Autowired BillRepository bills;
    @Autowired PaymentRepository payments;
    @Autowired RefundRepository refunds;
    @Autowired TransactionTemplate transaction;
    @MockitoBean NotificationService notifications;
    @MockitoBean(name = "razorpayRestClient") RestClient gateway;

    private Appointment appointment;
    private Payment payment;

    @BeforeEach
    void seedPaidAppointment() {
        String unique = UUID.randomUUID().toString();
        transaction.executeWithoutResult(status -> {
            Patient patient = patients.save(Patient.builder().firstName("Ava").lastName("Sharma")
                    .phone(unique).email(unique + "@example.test").build());
            Dentist dentist = dentists.save(Dentist.builder().name("Dr. Test").build());
            appointment = appointments.save(Appointment.builder().patient(patient).dentist(dentist)
                    .appointmentDate(LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(3))
                    .appointmentTime(LocalTime.of(11, 0)).status(AppointmentStatus.BOOKED).build());
            Bill bill = bills.save(Bill.builder().appointment(appointment).totalAmount(new BigDecimal("500.00"))
                    .discount(BigDecimal.ZERO).finalAmount(new BigDecimal("500.00")).build());
            payment = payments.save(Payment.builder().bill(bill).amount(new BigDecimal("500.00"))
                    .status(PaymentStatus.SUCCESS).paymentMode(PaymentMode.UPI)
                    .gatewayPaymentId("pay_" + unique).gatewayOrderId("order_" + unique).build());
        });
        authenticate(appointment.getPatient().getId());
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cancellationCommitsReleasedSlotAndPendingRefundWithoutCallingGateway() {
        assertTrue(service.getAppointmentById(appointment.getId()).isRefundEligible());
        var result = service.updateAppointment(appointment.getId(), cancellation());
        assertEquals(AppointmentStatus.CANCELLED, result.getStatus());
        assertEquals(1, result.getRefunds().size());
        assertEquals(RefundStatus.PENDING, result.getRefunds().getFirst().getStatus());
        assertEquals(new BigDecimal("500.00"), result.getRefunds().getFirst().getAmount());
        assertNull(result.getRefunds().getFirst().getRefundId());
        assertTrue(service.getAvailability(appointment.getDentist().getId(), appointment.getAppointmentDate())
                .getAvailableSlots().contains(appointment.getAppointmentTime()));
        assertEquals(PaymentStatus.SUCCESS, payments.findById(payment.getId()).orElseThrow().getStatus());
        verifyNoInteractions(gateway);
    }

    @Test
    void rollbackKeepsSlotBookedAndDiscardsRefundIntent() {
        transaction.executeWithoutResult(status -> {
            service.updateAppointment(appointment.getId(), cancellation());
            assertEquals(1, refunds.findByPaymentIdIn(List.of(payment.getId())).size());
            status.setRollbackOnly();
        });
        assertEquals(AppointmentStatus.BOOKED, appointments.findById(appointment.getId()).orElseThrow().getStatus());
        assertTrue(refunds.findByPaymentIdIn(List.of(payment.getId())).isEmpty());
        assertFalse(service.getAvailability(appointment.getDentist().getId(), appointment.getAppointmentDate())
                .getAvailableSlots().contains(appointment.getAppointmentTime()));
        verifyNoInteractions(gateway, notifications);
    }

    @Test
    void repeatedAndConcurrentCancellationProduceOneRefundIntent() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        Runnable cancel = () -> {
            authenticate(appointment.getPatient().getId());
            try {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                service.updateAppointment(appointment.getId(), cancellation());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(ex);
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
        var first = CompletableFuture.runAsync(cancel);
        var second = CompletableFuture.runAsync(cancel);
        start.countDown();
        CompletableFuture.allOf(first, second).get(10, TimeUnit.SECONDS);
        service.updateAppointment(appointment.getId(), cancellation());
        assertEquals(1, refunds.findByPaymentIdIn(List.of(payment.getId())).size());
        verifyNoInteractions(gateway);
    }

    @Test
    void anotherPatientCannotCancelAndPaidAppointmentsCannotBeDeletedOrReopened() {
        authenticate(appointment.getPatient().getId() + 10000);
        assertThrows(AccessDeniedException.class, () -> service.updateAppointment(appointment.getId(), cancellation()));
        authenticate(appointment.getPatient().getId());
        assertThrows(BadRequestException.class, () -> service.deleteAppointment(appointment.getId()));
        service.updateAppointment(appointment.getId(), cancellation());
        var reopening = cancellation();
        reopening.setStatus(AppointmentStatus.BOOKED);
        assertThrows(BadRequestException.class, () -> service.updateAppointment(appointment.getId(), reopening));
        assertEquals(1, refunds.findByPaymentIdIn(List.of(payment.getId())).size());
    }

    @ParameterizedTest
    @EnumSource(value = PaymentStatus.class, names = {"PENDING", "FAILED", "REFUNDED"})
    void onlySuccessfulPaymentsQueueAutomaticRefunds(PaymentStatus status) {
        transaction.executeWithoutResult(tx -> payments.findById(payment.getId()).orElseThrow().setStatus(status));
        assertFalse(service.getAppointmentById(appointment.getId()).isRefundEligible());
        var result = service.updateAppointment(appointment.getId(), cancellation());
        assertEquals(AppointmentStatus.CANCELLED, result.getStatus());
        assertTrue(result.getRefunds().isEmpty());
        assertTrue(refunds.findByPaymentIdIn(List.of(payment.getId())).isEmpty());
        verifyNoInteractions(gateway);
    }

    @Test
    void offlinePaymentsRemainRecordedAndRequireClinicHandling() {
        transaction.executeWithoutResult(tx -> {
            Payment recorded = payments.findById(payment.getId()).orElseThrow();
            recorded.setGatewayPaymentId(null);
            recorded.setGatewayOrderId(null);
            recorded.setPaymentMode(PaymentMode.CASH);
        });
        var before = service.getAppointmentById(appointment.getId());
        assertFalse(before.isRefundEligible());
        assertEquals(BigDecimal.ZERO, before.getRefundableAmount());
        assertEquals(new BigDecimal("500.00"), before.getPaidAmount());
        var result = service.updateAppointment(appointment.getId(), cancellation());
        assertTrue(result.getRefunds().isEmpty());
        assertEquals(PaymentStatus.SUCCESS, result.getPaymentStatus());
        verifyNoInteractions(gateway);
    }

    @Test
    void staffCannotChangeOrDeleteGatewayPaymentAndItsBill() {
        var admin = new CustomUserDetails(999L, "admin@example.test", "unused", Role.ADMIN, null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                admin, null, admin.getAuthorities()));
        Long billId = payment.getBill().getId();
        var changedBill = new BillRequestDto();
        changedBill.setAppointmentId(appointment.getId());
        assertThrows(BadRequestException.class, () -> billService.updateBill(billId, changedBill));
        assertThrows(BadRequestException.class, () -> billService.deleteBill(billId));
        assertThrows(BadRequestException.class, () -> paymentService.updatePayment(payment.getId(), new PaymentRequestDto()));
        assertThrows(BadRequestException.class, () -> paymentService.deletePayment(payment.getId()));
        assertEquals(PaymentStatus.SUCCESS, payments.findById(payment.getId()).orElseThrow().getStatus());
        assertTrue(bills.existsById(billId));
    }

    private AppointmentRequestDto cancellation() {
        return new AppointmentRequestDto(appointment.getPatient().getId(), appointment.getDentist().getId(),
                appointment.getAppointmentDate(), appointment.getAppointmentTime(), AppointmentStatus.CANCELLED, null);
    }

    private void authenticate(Long patientId) {
        var user = new CustomUserDetails(100L, "test@example.test", "unused", Role.PATIENT, patientId);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities()));
    }
}
