package com.gayatri.dentalclinic.entity;

import com.gayatri.dentalclinic.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

/** Durable refund intent, written in the same transaction as appointment cancellation. */
@Entity
@Table(name = "refunds", indexes = @Index(name = "idx_refund_due", columnList = "next_attempt_at"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refund {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus status;

    @Column(nullable = false, precision = 38, scale = 2, updatable = false)
    private BigDecimal amount;
    @Column(nullable = false, length = 3, updatable = false)
    private String currency;
    @Column(nullable = false, length = 40, unique = true, updatable = false)
    private String idempotencyKey;
    @Column(nullable = false, length = 40, unique = true, updatable = false)
    private String receipt;
    @Column(unique = true)
    private String gatewayRefundId;

    @Column(nullable = false, updatable = false)
    private Instant requestedAt;
    private Instant completedAt;
    private Instant submissionAttemptedAt;
    private Instant nextAttemptAt;
    private Instant leaseUntil;
    private String leaseToken;
    private int attempts;
    private Instant notificationSentAt;
    @Column(length = 512)
    private String lastError;
    @Version
    private Long version;
}
