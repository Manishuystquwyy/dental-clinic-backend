-- Apply once before deploying appointment refunds when Hibernate schema updates are disabled.
-- Do not reapply after ddl-auto=update has already added payments.currency.
-- Existing gateway bookings in this deployment were denominated in INR.
ALTER TABLE payments ADD COLUMN currency VARCHAR(3) NULL DEFAULT 'INR';
UPDATE payments SET currency = 'INR' WHERE currency IS NULL OR currency = '';

CREATE TABLE IF NOT EXISTS refunds (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    status ENUM('PENDING', 'REFUND_INITIATED', 'REFUNDED', 'FAILED') NOT NULL,
    amount DECIMAL(38,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    idempotency_key VARCHAR(40) NOT NULL,
    receipt VARCHAR(40) NOT NULL,
    gateway_refund_id VARCHAR(255),
    requested_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6),
    submission_attempted_at DATETIME(6),
    next_attempt_at DATETIME(6),
    lease_until DATETIME(6),
    lease_token VARCHAR(255),
    attempts INT NOT NULL DEFAULT 0,
    notification_sent_at DATETIME(6),
    last_error VARCHAR(512),
    version BIGINT,
    CONSTRAINT uk_refunds_payment UNIQUE (payment_id),
    CONSTRAINT uk_refunds_idempotency UNIQUE (idempotency_key),
    CONSTRAINT uk_refunds_receipt UNIQUE (receipt),
    CONSTRAINT uk_refunds_gateway_id UNIQUE (gateway_refund_id),
    INDEX idx_refund_due (next_attempt_at),
    CONSTRAINT fk_refund_payment FOREIGN KEY (payment_id) REFERENCES payments(id)
);

-- Do not backfill refunds for historical cancellations automatically.
-- Clinic staff must first check whether those payments were already refunded in Razorpay.
