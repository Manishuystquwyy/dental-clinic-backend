package com.gayatri.dentalclinic.service;

import com.gayatri.dentalclinic.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

@Component
public class BookingTime {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Kolkata");
    private final Clock clock;

    public BookingTime() {
        this(Clock.system(CLINIC_ZONE));
    }

    public BookingTime(Clock clock) {
        this.clock = clock.withZone(CLINIC_ZONE);
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public LocalDate dateAt(Instant instant) {
        return instant.atZone(CLINIC_ZONE).toLocalDate();
    }

    public void requireFuture(LocalDate date, LocalTime time) {
        if (date == null || time == null || !date.atTime(time).isAfter(now())) {
            throw new BadRequestException("Please choose a future appointment date and time.");
        }
    }

    public void requireOnlineChangeAllowed(LocalDate appointmentDate, LocalTime appointmentTime,
                                          int cutoffHours, String action) {
        if (cutoffHours < 0) {
            throw new IllegalArgumentException("Appointment cutoff hours must be nonnegative integers.");
        }
        if (appointmentDate == null || appointmentTime == null
                || now().isAfter(appointmentDate.atTime(appointmentTime).minusHours(cutoffHours))) {
            throw new BadRequestException(
                    "Appointments can only be " + action + " at least " + cutoffHours
                            + " hours before their scheduled start time.");
        }
    }
}
