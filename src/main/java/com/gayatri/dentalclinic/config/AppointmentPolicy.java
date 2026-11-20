package com.gayatri.dentalclinic.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record AppointmentPolicy(int cancellationCutoffHours, int rescheduleCutoffHours) {
    public AppointmentPolicy(
            @Value("${app.appointment.cancellation-cutoff-hours}") int cancellationCutoffHours,
            @Value("${app.appointment.reschedule-cutoff-hours}") int rescheduleCutoffHours) {
        if (cancellationCutoffHours < 0 || rescheduleCutoffHours < 0) {
            throw new IllegalArgumentException("Appointment cutoff hours must be nonnegative integers.");
        }
        this.cancellationCutoffHours = cancellationCutoffHours;
        this.rescheduleCutoffHours = rescheduleCutoffHours;
    }
}
