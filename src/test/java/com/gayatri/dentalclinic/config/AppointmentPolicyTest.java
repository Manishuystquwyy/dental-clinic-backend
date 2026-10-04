package com.gayatri.dentalclinic.config;

import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AppointmentPolicyTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(AppointmentPolicy.class);

    @Test
    void sharedConfigurationLoadsTheDefaultTwelveHourPolicies() throws IOException {
        var properties = new Properties();
        try (var resource = getClass().getResourceAsStream("/application.properties")) {
            assertNotNull(resource);
            properties.load(resource);
        }

        runner.withPropertyValues(
                "app.appointment.cancellation-cutoff-hours=" + properties.getProperty("app.appointment.cancellation-cutoff-hours"),
                "app.appointment.reschedule-cutoff-hours=" + properties.getProperty("app.appointment.reschedule-cutoff-hours"))
                .run(context -> {
                    assertThat(context).hasSingleBean(AppointmentPolicy.class);
                    assertThat(context.getBean(AppointmentPolicy.class)).isEqualTo(new AppointmentPolicy(12, 12));
                });
    }

    @ParameterizedTest
    @CsvSource({"24, 6", "6, 24", "0, 0"})
    void configurationAcceptsDistinctNonnegativeIntegralCutoffs(int cancellationHours, int rescheduleHours) {
        runner.withPropertyValues(
                "app.appointment.cancellation-cutoff-hours=" + cancellationHours,
                "app.appointment.reschedule-cutoff-hours=" + rescheduleHours)
                .run(context -> assertThat(context.getBean(AppointmentPolicy.class))
                        .isEqualTo(new AppointmentPolicy(cancellationHours, rescheduleHours)));
    }

    @ParameterizedTest
    @CsvSource({"-1, 12", "12, -1", "1.5, 12", "12, 1.5"})
    void configurationRejectsNegativeAndNonintegralCutoffs(String cancellationHours, String rescheduleHours) {
        runner.withPropertyValues(
                "app.appointment.cancellation-cutoff-hours=" + cancellationHours,
                "app.appointment.reschedule-cutoff-hours=" + rescheduleHours)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void configurationRequiresBothPropertiesWithoutJavaDefaults() {
        runner.withPropertyValues("app.appointment.cancellation-cutoff-hours=12")
                .run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("app.appointment.reschedule-cutoff-hours=12")
                .run(context -> assertThat(context).hasFailed());
    }
}
