package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.service.RefundGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.*;

@Component
public class RazorpayRefundGateway implements RefundGateway {
    private final RestClient client;
    private final String keyId;
    private final String keySecret;

    public RazorpayRefundGateway(RestClient client,
            @Value("${app.razorpay.key-id:}") String keyId,
            @Value("${app.razorpay.key-secret:}") String keySecret) {
        this.client = client;
        this.keyId = keyId;
        this.keySecret = keySecret;
    }

    @Override
    public Map<?, ?> fetchPayment(String paymentId) {
        requireConfiguration();
        return client.get().uri("/payments/{id}", paymentId).retrieve().body(Map.class);
    }

    @Override
    public Map<?, ?> fetchRefund(String refundId) {
        requireConfiguration();
        return client.get().uri("/refunds/{id}", refundId).retrieve().body(Map.class);
    }

    @Override
    public Optional<Map<?, ?>> findByReceipt(String paymentId, String receipt) {
        requireConfiguration();
        // The provider defaults to only ten results; paginate to recover lost POST responses.
        for (int skip = 0; skip < 10_000; skip += 100) {
            Map<?, ?> response = client.get().uri("/payments/{id}/refunds?count=100&skip={skip}",
                    paymentId, skip).retrieve().body(Map.class);
            if (response == null || !(response.get("items") instanceof List<?> items)) {
                throw new IllegalStateException("Invalid Razorpay refunds collection");
            }
            for (Object item : items) {
                if (item instanceof Map<?, ?> refund && receipt.equals(refund.get("receipt"))) {
                    return Optional.of(refund);
                }
            }
            if (items.size() < 100) {
                return Optional.empty();
            }
        }
        // Do not submit another request when the recovery lookup is incomplete.
        throw new IllegalStateException("Razorpay refund lookup exceeded pagination limit");
    }

    @Override
    public Map<?, ?> createRefund(String paymentId, long amount, String receipt, String idempotencyKey) {
        requireConfiguration();
        // Persisted values keep the complete body and key identical on every retry/restart.
        // https://razorpay.com/docs/api/refunds/normal-refunds-idempotent
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", amount);
        body.put("speed", "normal");
        body.put("receipt", receipt);
        return client.post().uri("/payments/{id}/refund", paymentId)
                .header("X-Refund-Idempotency", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve().body(Map.class);
    }

    private void requireConfiguration() {
        if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
            throw new IllegalStateException("Razorpay refund credentials are not configured");
        }
    }
}
