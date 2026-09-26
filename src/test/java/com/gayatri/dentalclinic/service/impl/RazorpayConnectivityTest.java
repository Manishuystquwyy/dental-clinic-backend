package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class RazorpayConnectivityTest {
    private final RestClient.Builder builder = RestClient.builder().baseUrl("https://api.razorpay.com/v1");
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    private RazorpayPaymentServiceImpl service() {
        var service = new RazorpayPaymentServiceImpl(null, null, null, null, null, null, null, null, builder.build());
        ReflectionTestUtils.setField(service, "razorpayKeyId", "test");
        ReflectionTestUtils.setField(service, "razorpayKeySecret", "test");
        return service;
    }

    @Test
    void transientConnectionFailureRetriesReadAndReturnsPayment() {
        server.expect(requestTo("https://api.razorpay.com/v1/payments/pay_test"))
                .andRespond(withException(new IOException("connection timed out")));
        server.expect(requestTo("https://api.razorpay.com/v1/payments/pay_test"))
                .andRespond(withSuccess("{\"id\":\"pay_test\",\"status\":\"captured\"}", MediaType.APPLICATION_JSON));
        Map<?, ?> payment = ReflectionTestUtils.invokeMethod(service(), "fetchPayment", "pay_test");
        assertEquals("pay_test", payment.get("id"));
        server.verify();
    }

    @Test
    void repeatedConnectionFailureStopsAfterOneRetryAndWarnsAgainstPayingAgain() {
        for (int i = 0; i < 2; i++) {
            server.expect(requestTo("https://api.razorpay.com/v1/payments/pay_test"))
                    .andRespond(withException(new IOException("connection timed out")));
        }
        var error = assertThrows(BadRequestException.class,
                () -> ReflectionTestUtils.invokeMethod(service(), "fetchPayment", "pay_test"));
        assertTrue(error.getMessage().contains("do not pay again"));
        server.verify();
    }

    @Test
    void gatewayRejectionIsNotRetried() {
        server.expect(requestTo("https://api.razorpay.com/v1/payments/pay_test"))
                .andRespond(withBadRequest());
        assertThrows(BadRequestException.class,
                () -> ReflectionTestUtils.invokeMethod(service(), "fetchPayment", "pay_test"));
        server.verify();
    }
}
