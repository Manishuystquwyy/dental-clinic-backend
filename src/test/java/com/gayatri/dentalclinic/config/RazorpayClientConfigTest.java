package com.gayatri.dentalclinic.config;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

class RazorpayClientConfigTest {
    @Test
    void unresponsiveGatewayCannotHoldRequestIndefinitely() throws Exception {
        var release = new CountDownLatch(1);
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/payments/test", exchange -> {
            try { release.await(); }
            catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            finally { exchange.close(); }
        });
        server.start();
        try {
            var client = new RazorpayClientConfig().razorpayRestClient("test", "test",
                    Duration.ofSeconds(1), Duration.ofMillis(200)).mutate()
                    .baseUrl("http://127.0.0.1:" + server.getAddress().getPort()).build();
            assertTimeoutPreemptively(Duration.ofSeconds(3), () ->
                    assertThrows(ResourceAccessException.class,
                            () -> client.get().uri("/payments/test").retrieve().body(String.class)));
        } finally {
            release.countDown();
            server.stop(0);
        }
    }

    @Test
    void infiniteTimeoutConfigurationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RazorpayClientConfig()
                .razorpayRestClient("test", "test", Duration.ofSeconds(5), Duration.ZERO));
    }
}
