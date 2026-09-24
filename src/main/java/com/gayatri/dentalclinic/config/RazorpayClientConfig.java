package com.gayatri.dentalclinic.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RazorpayClientConfig {
    @Bean
    public RestClient razorpayRestClient(
            @Value("${app.razorpay.key-id:}") String keyId,
            @Value("${app.razorpay.key-secret:}") String keySecret,
            @Value("${app.razorpay.connect-timeout:5s}") Duration connectTimeout,
            @Value("${app.razorpay.read-timeout:10s}") Duration readTimeout) {
        if (connectTimeout.isZero() || connectTimeout.isNegative()
                || readTimeout.isZero() || readTimeout.isNegative()) {
            throw new IllegalArgumentException("Razorpay timeouts must be positive");
        }
        // Reuse connections and bound gateway waits below the reverse proxy timeout.
        var httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        var factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        return RestClient.builder()
                .baseUrl("https://api.razorpay.com/v1")
                .requestFactory(factory)
                .defaultHeaders(headers -> headers.setBasicAuth(keyId, keySecret))
                .build();
    }
}
