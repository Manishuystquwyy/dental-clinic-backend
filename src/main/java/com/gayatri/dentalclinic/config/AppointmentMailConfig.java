package com.gayatri.dentalclinic.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AppointmentMailConfig {

    @Bean
    public ThreadPoolTaskExecutor appointmentMailExecutor(
            @Value("${app.mail.workers:2}") int workers,
            @Value("${app.mail.queue-capacity:100}") int queueCapacity) {
        if (workers < 1 || queueCapacity < 1) {
            throw new IllegalArgumentException("Mail workers and queue capacity must be positive");
        }
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(workers);
        executor.setMaxPoolSize(workers);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("appointment-mail-");
        // CallerRunsPolicy would put slow SMTP back on the booking request thread.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }
}
