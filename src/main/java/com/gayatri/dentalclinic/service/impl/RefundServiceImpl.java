package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.entity.*;
import com.gayatri.dentalclinic.enums.*;
import com.gayatri.dentalclinic.exception.BadRequestException;
import com.gayatri.dentalclinic.repository.*;
import com.gayatri.dentalclinic.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientResponseException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
@Slf4j
public class RefundServiceImpl implements RefundService {
    private final PaymentRepository payments;
    private final RefundRepository refunds;
    private final RefundGateway gateway;
    private final NotificationService notifications;
    private final BookingTime bookingTime;
    private final TransactionTemplate transactions;
    private final long leaseSeconds;

    public RefundServiceImpl(PaymentRepository payments, RefundRepository refunds,
            RefundGateway gateway, NotificationService notifications, BookingTime bookingTime,
            PlatformTransactionManager transactionManager,
            @Value("${app.refunds.lease-seconds:180}") long leaseSeconds) {
        this.payments = payments;
        this.refunds = refunds;
        this.gateway = gateway;
        this.notifications = notifications;
        this.bookingTime = bookingTime;
        if (leaseSeconds < 30) {
            throw new IllegalArgumentException("Refund lease must be at least 30 seconds");
        }
        this.leaseSeconds = leaseSeconds;
        this.transactions = new TransactionTemplate(transactionManager);
        this.transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void queueCancellation(Appointment appointment) {
        if (appointment.getStatus() != AppointmentStatus.CANCELLED) {
            throw new IllegalArgumentException("Refunds require a cancelled appointment");
        }
        for (Payment payment : payments.findByBillAppointmentId(appointment.getId())) {
            if (payment.getStatus() != PaymentStatus.SUCCESS || blank(payment.getGatewayPaymentId())
                    || payment.getAmount() == null || payment.getAmount().signum() <= 0) {
                continue;
            }
            Optional<Refund> existing = refunds.findByPaymentId(payment.getId());
            if (existing.isPresent()) {
                Refund refund = refunds.findWithLockById(existing.get().getId()).orElseThrow();
                if (refund.getStatus() == RefundStatus.PENDING && refund.getLeaseUntil() == null
                        && refund.getNextAttemptAt() != null && refund.getNextAttemptAt().isAfter(now())) {
                    // A late payment.captured event resumes a cancellation waiting for capture promptly.
                    refund.setNextAttemptAt(now());
                }
                continue;
            }
            Instant requestedAt = now();
            String key = UUID.randomUUID().toString();
            refunds.save(Refund.builder().payment(payment).status(RefundStatus.PENDING)
                    .amount(payment.getAmount()).currency(blank(payment.getCurrency()) ? "INR" : payment.getCurrency())
                    .idempotencyKey(key).receipt("gdc_" + key.replace("-", ""))
                    .requestedAt(requestedAt).nextAttemptAt(requestedAt).build());
        }
    }

    @Override
    @Transactional
    public void handleWebhook(String event, Map<?, ?> entity) {
        if (!Set.of("refund.created", "refund.processed", "refund.failed").contains(event)) {
            return;
        }
        String paymentId = text(entity, "payment_id");
        if (paymentId.isBlank()) {
            throw new BadRequestException("Invalid Razorpay refund webhook payload");
        }
        refunds.findWithLockByGatewayPaymentId(paymentId).ifPresent(refund -> {
            // An unrelated dashboard refund must never complete this refund request.
            if (!Objects.equals(refund.getGatewayRefundId(), text(entity, "id"))
                    && !Objects.equals(refund.getReceipt(), text(entity, "receipt"))) {
                return;
            }
            try {
                applyProviderState(refund, entity);
            } catch (InvalidProviderEntity ex) {
                throw new BadRequestException(ex.getMessage());
            }
        });
    }

    @Override
    public void processDueRefunds() {
        List<Long> dueIds = refunds.findDueIds(now(), PageRequest.of(0, 50));
        for (Long id : dueIds) {
            try {
                Work work = transactions.execute(tx -> claim(id));
                if (work != null) {
                    process(work);
                }
            } catch (RuntimeException ex) {
                // One invalid row or transaction conflict must not stall the durable queue.
                log.warn("Refund worker could not process refund request {}", id, ex);
            }
        }
    }

    private Work claim(Long id) {
        Refund refund = refunds.findWithLockById(id).orElse(null);
        Instant instant = now();
        if (refund == null || refund.getNextAttemptAt() == null || refund.getNextAttemptAt().isAfter(instant)
                || (refund.getLeaseUntil() != null && refund.getLeaseUntil().isAfter(instant))) {
            return null;
        }
        if (refund.getStatus() == RefundStatus.FAILED
                || (refund.getStatus() == RefundStatus.REFUNDED && refund.getNotificationSentAt() != null)) {
            refund.setNextAttemptAt(null);
            return null;
        }
        String token = UUID.randomUUID().toString();
        refund.setLeaseToken(token);
        refund.setLeaseUntil(instant.plusSeconds(leaseSeconds));
        refund.setAttempts(refund.getAttempts() + 1);
        Payment payment = refund.getPayment();
        Appointment appointment = payment.getBill().getAppointment();
        return new Work(refund.getId(), token, refund.getStatus(), payment.getGatewayPaymentId(),
                payment.getGatewayOrderId(), appointment.getId(), appointment.getPatient().getEmail(),
                refund.getAmount(), refund.getCurrency(), refund.getReceipt(), refund.getIdempotencyKey(),
                refund.getGatewayRefundId(), refund.getAttempts(), refund.getSubmissionAttemptedAt() != null);
    }

    private void process(Work work) {
        if (work.status() == RefundStatus.REFUNDED) {
            notifyPatient(work);
            return;
        }
        try {
            Map<?, ?> providerRefund;
            boolean creationResult = false;
            if (!blank(work.refundId())) {
                providerRefund = gateway.fetchRefund(work.refundId());
            } else {
                Optional<Map<?, ?>> recovered = gateway.findByReceipt(work.paymentId(), work.receipt());
                if (recovered.isPresent()) {
                    providerRefund = recovered.get();
                } else if (work.submitted()) {
                    // The original POST may already have completed while collection/payment reads lag.
                    // Reuse the persisted request; Razorpay returns the original refund without duplication.
                    providerRefund = gateway.createRefund(work.paymentId(), paise(work.amount()),
                            work.receipt(), work.idempotencyKey());
                    creationResult = true;
                } else {
                    Map<?, ?> payment = gateway.fetchPayment(work.paymentId());
                    validatePayment(work, payment);
                    String status = text(payment, "status");
                    if ("authorized".equals(status)) {
                        retry(work, "Waiting for Razorpay to capture the payment", false);
                        return;
                    }
                    if (!"captured".equals(status)) {
                        throw new InvalidProviderEntity("Payment is not captured; clinic review is required");
                    }
                    long alreadyRefunded = number(payment, "amount_refunded");
                    if (alreadyRefunded > 0) {
                        // Full cancellation refunds may not overwrite independent partial refunds.
                        throw new InvalidProviderEntity("Payment already has another refund; clinic review is required");
                    }
                    // The lease has already committed. No database transaction spans the provider call.
                    Boolean stillOwned = transactions.execute(tx -> {
                        Refund refund = owned(work);
                        if (refund == null || refund.getStatus() == RefundStatus.REFUNDED
                                || refund.getStatus() == RefundStatus.FAILED) {
                            return false;
                        }
                        refund.setSubmissionAttemptedAt(now());
                        return true;
                    });
                    if (!Boolean.TRUE.equals(stillOwned)) {
                        return;
                    }
                    providerRefund = gateway.createRefund(work.paymentId(), paise(work.amount()),
                            work.receipt(), work.idempotencyKey());
                    creationResult = true;
                }
            }
            boolean isCreationResult = creationResult;
            transactions.executeWithoutResult(tx -> {
                Refund refund = owned(work);
                if (refund != null) {
                    if (isCreationResult && "processed".equals(text(providerRefund, "status"))) {
                        // Confirm provider completion with a signed webhook or a subsequent read.
                        Map<Object, Object> initiated = new HashMap<>();
                        providerRefund.forEach(initiated::put);
                        initiated.put("status", "pending");
                        applyProviderState(refund, initiated);
                    } else {
                        applyProviderState(refund, providerRefund);
                    }
                    release(refund);
                }
            });
        } catch (InvalidProviderEntity ex) {
            retry(work, ex.getMessage(), true);
        } catch (RestClientResponseException ex) {
            // 409 means the original request is still processing. Same-key retries are safe.
            boolean definitive = ex.getStatusCode().value() == 400 || ex.getStatusCode().value() == 422;
            retry(work, "Razorpay returned HTTP " + ex.getStatusCode().value()
                    + (definitive ? "; clinic review is required" : "; will retry automatically"), definitive);
        } catch (RuntimeException ex) {
            log.warn("Refund request {} will retry after provider failure ({})", work.id(),
                    ex.getClass().getSimpleName());
            retry(work, "Refund processing is temporarily unavailable; will retry automatically", false);
        }
    }

    private void validatePayment(Work work, Map<?, ?> payment) {
        if (!work.paymentId().equals(text(payment, "id"))
                || (!blank(work.orderId()) && !work.orderId().equals(text(payment, "order_id")))
                || !work.currency().equals(text(payment, "currency"))
                || paise(work.amount()) != number(payment, "amount")) {
            throw new InvalidProviderEntity("Razorpay payment details do not match the refund request");
        }
    }

    private void applyProviderState(Refund refund, Map<?, ?> entity) {
        String id = text(entity, "id");
        if (id.isBlank() || !refund.getPayment().getGatewayPaymentId().equals(text(entity, "payment_id"))
                || !refund.getCurrency().equals(text(entity, "currency"))
                || paise(refund.getAmount()) != number(entity, "amount")
                || (!blank(refund.getGatewayRefundId()) && !refund.getGatewayRefundId().equals(id))
                || (blank(refund.getGatewayRefundId()) && !refund.getReceipt().equals(text(entity, "receipt")))) {
            throw new InvalidProviderEntity("Razorpay refund details do not match the refund request");
        }
        String providerStatus = text(entity, "status");
        if (!Set.of("pending", "processed", "failed").contains(providerStatus)) {
            throw new InvalidProviderEntity("Razorpay refund status is invalid");
        }
        refund.setGatewayRefundId(id);
        // Processed is terminal. Delayed created/failed webhooks cannot undo confirmed money movement.
        if (refund.getStatus() == RefundStatus.REFUNDED) {
            return;
        }
        if ("processed".equals(providerStatus)) {
            refund.setStatus(RefundStatus.REFUNDED);
            refund.setCompletedAt(now());
            refund.getPayment().setStatus(PaymentStatus.REFUNDED);
            refund.setLastError(null);
            refund.setNextAttemptAt(now()); // A separate worker turn sends email after commit.
            release(refund);
        } else if ("failed".equals(providerStatus)) {
            refund.setStatus(RefundStatus.FAILED);
            refund.setLastError("Razorpay could not process this refund; contact the clinic");
            refund.setNextAttemptAt(null);
            release(refund);
        } else if (refund.getStatus() != RefundStatus.FAILED) {
            refund.setStatus(RefundStatus.REFUND_INITIATED);
            refund.setLastError(null);
            refund.setNextAttemptAt(now().plusSeconds(60));
        }
    }

    private void notifyPatient(Work work) {
        try {
            boolean sent = notifications.sendRefundConfirmation(work.email(), work.appointmentId(),
                    work.amount(), work.currency(), work.refundId());
            transactions.executeWithoutResult(tx -> {
                Refund refund = owned(work);
                if (refund != null) {
                    if (sent) {
                        refund.setNotificationSentAt(now());
                        refund.setNextAttemptAt(null);
                        refund.setLastError(null);
                    } else {
                        refund.setNextAttemptAt(now().plusSeconds(3600));
                        refund.setLastError("Refund confirmed; notification delivery is awaiting email configuration");
                    }
                    release(refund);
                }
            });
        } catch (RuntimeException ex) {
            retry(work, "Refund confirmed; notification delivery will retry", false);
        }
    }

    private void retry(Work work, String error, boolean terminal) {
        transactions.executeWithoutResult(tx -> {
            Refund refund = owned(work);
            if (refund == null) {
                return;
            }
            if (terminal && refund.getStatus() != RefundStatus.REFUNDED) {
                refund.setStatus(RefundStatus.FAILED);
                refund.setNextAttemptAt(null);
            } else {
                long delay = Math.min(3600, 60L * (1L << Math.min(6, work.attempts() - 1)));
                refund.setNextAttemptAt(now().plusSeconds(delay));
            }
            refund.setLastError(error);
            release(refund);
        });
    }

    private Refund owned(Work work) {
        return refunds.findWithLockById(work.id())
                .filter(refund -> work.token().equals(refund.getLeaseToken())).orElse(null);
    }

    private void release(Refund refund) {
        refund.setLeaseToken(null);
        refund.setLeaseUntil(null);
    }

    private Instant now() {
        return bookingTime.now().atZone(ZoneId.of("Asia/Kolkata")).toInstant();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String text(Map<?, ?> entity, String key) {
        return entity == null || entity.get(key) == null ? "" : String.valueOf(entity.get(key));
    }

    private long number(Map<?, ?> entity, String key) {
        try {
            return new BigDecimal(text(entity, key)).longValueExact();
        } catch (NumberFormatException | ArithmeticException ex) {
            throw new InvalidProviderEntity("Invalid Razorpay " + key);
        }
    }

    private long paise(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }

    private record Work(Long id, String token, RefundStatus status, String paymentId, String orderId,
                        Long appointmentId, String email, BigDecimal amount, String currency, String receipt,
                        String idempotencyKey, String refundId, int attempts, boolean submitted) {}

    private static class InvalidProviderEntity extends IllegalStateException {
        InvalidProviderEntity(String message) {
            super(message);
        }
    }
}
