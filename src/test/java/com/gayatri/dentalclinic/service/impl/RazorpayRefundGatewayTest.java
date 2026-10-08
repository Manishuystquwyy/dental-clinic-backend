package com.gayatri.dentalclinic.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RazorpayRefundGatewayTest {
    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://api.razorpay.com/v1");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    @Test
    void repeatedRefundPostUsesSameStableProviderIdempotencyHeaderAndBody() {
        String expectedBody = "{\"amount\":50000,\"speed\":\"normal\",\"receipt\":\"gdc_receipt\"}";
        for (int attempt = 0; attempt < 2; attempt++) {
            server.expect(requestTo("https://api.razorpay.com/v1/payments/pay_test/refund"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(header("X-Refund-Idempotency", "stable-request-key-123"))
                    .andExpect(content().string(expectedBody))
                    .andRespond(withSuccess("{\"id\":\"rfnd_test\"}", MediaType.APPLICATION_JSON));
        }
        for (int attempt = 0; attempt < 2; attempt++) {
            var gateway = new RazorpayRefundGateway(builder.build(), "test", "test");
            assertEquals("rfnd_test", gateway.createRefund("pay_test", 50000, "gdc_receipt", "stable-request-key-123").get("id"));
        }
        server.verify();
    }

    @Test
    void recoveryPaginatesPastTheFirstHundredRefunds() {
        String items = IntStream.range(0, 100).mapToObj(i -> "{\"receipt\":\"other-" + i + "\"}")
                .collect(Collectors.joining(","));
        server.expect(requestTo("https://api.razorpay.com/v1/payments/pay_test/refunds?count=100&skip=0"))
                .andRespond(withSuccess("{\"items\":[" + items + "]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.razorpay.com/v1/payments/pay_test/refunds?count=100&skip=100"))
                .andRespond(withSuccess("{\"items\":[{\"id\":\"rfnd_test\",\"receipt\":\"gdc_receipt\"}]}", MediaType.APPLICATION_JSON));
        var gateway = new RazorpayRefundGateway(builder.build(), "test", "test");
        assertEquals("rfnd_test", gateway.findByReceipt("pay_test", "gdc_receipt").orElseThrow().get("id"));
        server.verify();
    }
}
