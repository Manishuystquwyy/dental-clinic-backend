package com.gayatri.dentalclinic.service;

import java.util.Map;
import java.util.Optional;

public interface RefundGateway {
    Map<?, ?> fetchPayment(String paymentId);
    Map<?, ?> fetchRefund(String refundId);
    Optional<Map<?, ?>> findByReceipt(String paymentId, String receipt);
    Map<?, ?> createRefund(String paymentId, long amount, String receipt, String idempotencyKey);
}
