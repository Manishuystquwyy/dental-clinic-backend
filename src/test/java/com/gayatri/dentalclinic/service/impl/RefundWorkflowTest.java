package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.entity.*;
import com.gayatri.dentalclinic.enums.*;
import com.gayatri.dentalclinic.exception.BadRequestException;
import com.gayatri.dentalclinic.repository.*;
import com.gayatri.dentalclinic.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.ResourceAccessException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:refund-worker;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false", "spring.jpa.open-in-view=false", "app.admin.email=", "app.admin.password=",
        "app.refunds.enabled=false", "app.refunds.worker-enabled=false"
})
class RefundWorkflowTest {
    @Autowired RefundService service;
    @Autowired PaymentRepository payments;
    @Autowired RefundRepository refunds;
    @Autowired AppointmentRepository appointments;
    @Autowired BillRepository bills;
    @Autowired PatientRepository patients;
    @Autowired DentistRepository dentists;
    @Autowired TransactionTemplate transaction;
    @Autowired PlatformTransactionManager manager;
    @MockitoBean RefundGateway gateway;
    @MockitoBean NotificationService notifications;
    @MockitoBean BookingTime bookingTime;
    private final AtomicReference<Instant> instant = new AtomicReference<>();
    private Payment payment;

    @BeforeEach
    void seedPendingRefund() {
        refunds.deleteAll();
        instant.set(Instant.parse("2026-10-04T15:10:00Z"));
        when(bookingTime.now()).thenAnswer(call -> LocalDateTime.ofInstant(instant.get(), ZoneId.of("Asia/Kolkata")));
        transaction.executeWithoutResult(tx -> {
            String unique = UUID.randomUUID().toString();
            Patient patient = patients.save(Patient.builder().firstName("Ava").lastName("Sharma")
                    .phone(unique).email(unique + "@example.test").build());
            Dentist dentist = dentists.save(Dentist.builder().name("Dr. Test").build());
            Appointment appointment = appointments.save(Appointment.builder().patient(patient).dentist(dentist)
                    .appointmentDate(LocalDate.of(2026, 10, 7)).appointmentTime(LocalTime.of(11, 0))
                    .status(AppointmentStatus.CANCELLED).build());
            Bill bill = bills.save(Bill.builder().appointment(appointment).finalAmount(new BigDecimal("500.00")).build());
            payment = payments.save(Payment.builder().bill(bill).amount(new BigDecimal("500.00"))
                    .status(PaymentStatus.SUCCESS).currency("INR").gatewayPaymentId("pay_" + unique)
                    .gatewayOrderId("order_" + unique).build());
            service.queueCancellation(appointment);
        });
        when(gateway.findByReceipt(anyString(), anyString())).thenReturn(Optional.empty());
        when(gateway.fetchPayment(payment.getGatewayPaymentId())).thenAnswer(call -> providerPayment("captured", 0));
        when(notifications.sendRefundConfirmation(anyString(), anyLong(), any(), anyString(), anyString())).thenReturn(true);
    }

    @Test
    void postOnlyInitiatesThenReconciliationCommitsRefundAndDeliversOneEmail() {
        when(gateway.createRefund(anyString(), anyLong(), anyString(), anyString())).thenAnswer(call -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            return providerRefund("processed");
        });
        service.processDueRefunds();
        assertEquals(RefundStatus.REFUND_INITIATED, current().getStatus());
        assertEquals(PaymentStatus.SUCCESS, payments.findById(payment.getId()).orElseThrow().getStatus());
        verifyNoInteractions(notifications);
        when(gateway.fetchRefund("rfnd_test")).thenAnswer(call -> providerRefund("processed"));
        advance(61);
        service.processDueRefunds();
        assertEquals(RefundStatus.REFUNDED, current().getStatus());
        assertEquals(PaymentStatus.REFUNDED, payments.findById(payment.getId()).orElseThrow().getStatus());
        assertNotNull(current().getCompletedAt());
        service.processDueRefunds();
        assertNotNull(current().getNotificationSentAt());
        service.handleWebhook("refund.created", providerRefund("pending"));
        service.handleWebhook("refund.failed", providerRefund("failed"));
        service.handleWebhook("refund.processed", providerRefund("processed"));
        service.processDueRefunds();
        assertEquals(RefundStatus.REFUNDED, current().getStatus());
        verify(notifications, times(1)).sendRefundConfirmation(anyString(), anyLong(), any(), anyString(), eq("rfnd_test"));
    }

    @Test
    void timeoutAndRestartRetryIdenticalRequestEvenWhenPaymentReadsLag() {
        String key = current().getIdempotencyKey();
        String receipt = current().getReceipt();
        when(gateway.createRefund(payment.getGatewayPaymentId(), 50000, receipt, key))
                .thenThrow(new ResourceAccessException("lost response"))
                .thenAnswer(call -> providerRefund("processed"));
        service.processDueRefunds();
        assertEquals(RefundStatus.PENDING, current().getStatus());
        assertNotNull(current().getSubmissionAttemptedAt());
        advance(61);
        when(gateway.fetchPayment(payment.getGatewayPaymentId())).thenAnswer(call -> providerPayment("refunded", 50000));
        var restarted = new RefundServiceImpl(payments, refunds, gateway, notifications, bookingTime, manager, 180);
        restarted.processDueRefunds();
        assertEquals(RefundStatus.REFUND_INITIATED, current().getStatus());
        assertEquals(key, current().getIdempotencyKey());
        verify(gateway, times(2)).createRefund(payment.getGatewayPaymentId(), 50000, receipt, key);
        verify(gateway, times(1)).fetchPayment(payment.getGatewayPaymentId());
    }

    @Test
    void receiptRecoveryConfirmsLostPostWithoutSubmittingAgain() {
        when(gateway.createRefund(anyString(), anyLong(), anyString(), anyString()))
                .thenThrow(new ResourceAccessException("lost response"));
        service.processDueRefunds();
        advance(61);
        when(gateway.findByReceipt(payment.getGatewayPaymentId(), current().getReceipt()))
                .thenReturn(Optional.of(providerRefund("processed")));
        service.processDueRefunds();
        assertEquals(RefundStatus.REFUNDED, current().getStatus());
        verify(gateway, times(1)).createRefund(anyString(), anyLong(), anyString(), anyString());
    }

    @Test
    void webhookBeforePostResponseWinsWithoutBeingRegressedByWorker() {
        when(gateway.createRefund(anyString(), anyLong(), anyString(), anyString())).thenAnswer(call -> {
            service.handleWebhook("refund.processed", providerRefund("processed"));
            return providerRefund("pending");
        });
        service.processDueRefunds();
        assertEquals(RefundStatus.REFUNDED, current().getStatus());
        assertNull(current().getLeaseToken());
        service.processDueRefunds();
        verify(notifications, times(1)).sendRefundConfirmation(anyString(), anyLong(), any(), anyString(), anyString());
    }

    @Test
    void twoWorkersCannotConcurrentlySubmitTheSameRequest() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        when(gateway.createRefund(anyString(), anyLong(), anyString(), anyString())).thenAnswer(call -> {
            started.countDown();
            assertTrue(finish.await(10, TimeUnit.SECONDS));
            return providerRefund("pending");
        });
        CompletableFuture<Void> first = CompletableFuture.runAsync(service::processDueRefunds);
        try {
            assertTrue(started.await(10, TimeUnit.SECONDS));
            service.processDueRefunds();
        } finally {
            finish.countDown();
        }
        first.get(10, TimeUnit.SECONDS);
        assertEquals(RefundStatus.REFUND_INITIATED, current().getStatus());
        verify(gateway, times(1)).createRefund(anyString(), anyLong(), anyString(), anyString());
    }

    @Test
    void expiredWorkerLeaseRecoversAfterCrash() {
        transaction.executeWithoutResult(tx -> {
            Refund refund = refunds.findWithLockById(current().getId()).orElseThrow();
            refund.setLeaseToken("crashed-worker");
            refund.setLeaseUntil(instant.get().plusSeconds(180));
        });
        service.processDueRefunds();
        verifyNoInteractions(gateway);
        advance(181);
        when(gateway.createRefund(anyString(), anyLong(), anyString(), anyString())).thenAnswer(call -> providerRefund("pending"));
        service.processDueRefunds();
        assertEquals(RefundStatus.REFUND_INITIATED, current().getStatus());
    }

    @Test
    void authorizedPaymentWaitsForCaptureWithoutSendingRefund() {
        when(gateway.fetchPayment(payment.getGatewayPaymentId())).thenAnswer(call -> providerPayment("authorized", 0));
        service.processDueRefunds();
        assertEquals(RefundStatus.PENDING, current().getStatus());
        assertNull(current().getSubmissionAttemptedAt());
        assertTrue(current().getNextAttemptAt().isAfter(instant.get()));
        verify(gateway, never()).createRefund(anyString(), anyLong(), anyString(), anyString());
    }

    @Test
    void lateCaptureWakesExistingCancellationWithoutCreatingAnotherRefundIntent() {
        when(gateway.fetchPayment(payment.getGatewayPaymentId())).thenAnswer(call -> providerPayment("authorized", 0));
        service.processDueRefunds();
        assertTrue(current().getNextAttemptAt().isAfter(instant.get()));
        String originalKey = current().getIdempotencyKey();
        transaction.executeWithoutResult(tx -> service.queueCancellation(
                appointments.findById(payment.getBill().getAppointment().getId()).orElseThrow()));
        assertEquals(instant.get(), current().getNextAttemptAt());
        assertEquals(originalKey, current().getIdempotencyKey());
        assertEquals(1, refunds.count());
        when(gateway.fetchPayment(payment.getGatewayPaymentId())).thenAnswer(call -> providerPayment("captured", 0));
        when(gateway.createRefund(anyString(), anyLong(), anyString(), anyString())).thenAnswer(call -> providerRefund("pending"));
        service.processDueRefunds();
        assertEquals(RefundStatus.REFUND_INITIATED, current().getStatus());
    }

    @Test
    void mismatchedWebhookCannotChangePaymentOrRefund() {
        for (Map.Entry<String, Object> mismatch : Map.<String, Object>of("amount", 1, "currency", "USD").entrySet()) {
            Map<String, Object> entity = providerRefund("processed");
            entity.put(mismatch.getKey(), mismatch.getValue());
            assertThrows(BadRequestException.class, () -> service.handleWebhook("refund.processed", entity));
        }
        Map<String, Object> unrelated = providerRefund("processed");
        unrelated.put("receipt", "unrelated");
        service.handleWebhook("refund.processed", unrelated);
        assertEquals(RefundStatus.PENDING, current().getStatus());
        assertEquals(PaymentStatus.SUCCESS, payments.findById(payment.getId()).orElseThrow().getStatus());
    }

    @Test
    void providerFailureKeepsAppointmentCancelledAndMoneyLedgerUnrefunded() {
        when(gateway.createRefund(anyString(), anyLong(), anyString(), anyString())).thenAnswer(call -> providerRefund("failed"));
        service.processDueRefunds();
        assertEquals(RefundStatus.FAILED, current().getStatus());
        assertNull(current().getNextAttemptAt());
        assertEquals(PaymentStatus.SUCCESS, payments.findById(payment.getId()).orElseThrow().getStatus());
        assertEquals(AppointmentStatus.CANCELLED, appointments.findById(payment.getBill().getAppointment().getId()).orElseThrow().getStatus());
        verifyNoInteractions(notifications);
    }

    @Test
    void failedEmailRetriesWithoutChangingConfirmedRefundOrResubmittingPayment() {
        service.handleWebhook("refund.processed", providerRefund("processed"));
        when(notifications.sendRefundConfirmation(anyString(), anyLong(), any(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("SMTP unavailable")).thenReturn(true);
        service.processDueRefunds();
        assertEquals(RefundStatus.REFUNDED, current().getStatus());
        assertNull(current().getNotificationSentAt());
        advance(61);
        service.processDueRefunds();
        assertNotNull(current().getNotificationSentAt());
        verifyNoInteractions(gateway);
    }

    private Refund current() {
        return refunds.findByPaymentId(payment.getId()).orElseThrow();
    }

    private Map<String, Object> providerRefund(String status) {
        return new HashMap<>(Map.of("id", "rfnd_test", "payment_id", payment.getGatewayPaymentId(),
                "currency", "INR", "amount", 50000, "receipt", current().getReceipt(), "status", status));
    }

    private Map<String, Object> providerPayment(String status, long refundedAmount) {
        return new HashMap<>(Map.of("id", payment.getGatewayPaymentId(), "order_id", payment.getGatewayOrderId(),
                "currency", "INR", "amount", 50000, "amount_refunded", refundedAmount, "status", status));
    }

    private void advance(long seconds) {
        instant.updateAndGet(value -> value.plusSeconds(seconds));
    }
}
