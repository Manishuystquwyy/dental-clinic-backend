package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.exception.BadRequestException;
import com.gayatri.dentalclinic.service.RefundService;
import org.junit.jupiter.api.Test;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RazorpayRefundWebhookTest {
    private final RefundService refunds = mock(RefundService.class);
    private final RazorpayPaymentServiceImpl service = new RazorpayPaymentServiceImpl(null, null, null,
            null, null, null, null, null, refunds, null, "", "", "INR", "test-webhook-secret");
    private final String payload = """
            {"event":"refund.processed","payload":{"refund":{"entity":{
            "id":"rfnd_test","payment_id":"pay_test","amount":50000,"currency":"INR","status":"processed"}}}}
            """;

    @Test
    void validSignedRefundEventRoutesToRefundStateMachine() throws Exception {
        service.handleWebhook(payload, sign(payload));
        verify(refunds).handleWebhook(eq("refund.processed"), argThat(entity ->
                "rfnd_test".equals(entity.get("id")) && "pay_test".equals(entity.get("payment_id"))));
    }

    @Test
    void forgedMissingMalformedOrChangedPayloadCannotReachRefundService() throws Exception {
        for (String signature : new String[] {null, "", "invalid", "0".repeat(64)}) {
            assertThrows(BadRequestException.class, () -> service.handleWebhook(payload, signature));
        }
        String valid = sign(payload);
        assertThrows(BadRequestException.class, () -> service.handleWebhook(payload.replace("50000", "60000"), valid));
        verifyNoInteractions(refunds);
    }

    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-webhook-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
}
