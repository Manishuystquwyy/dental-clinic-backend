package com.gayatri.dentalclinic.config;

import com.gayatri.dentalclinic.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(name = {"app.refunds.enabled", "app.refunds.worker-enabled"},
        havingValue = "true", matchIfMissing = true)
public class RefundWorkerConfig {
    private final RefundService refundService;

    @Scheduled(fixedDelayString = "${app.refunds.poll-interval-ms:15000}",
            initialDelayString = "${app.refunds.initial-delay-ms:15000}")
    public void processRefunds() {
        refundService.processDueRefunds();
    }
}
