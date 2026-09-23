package com.gayatri.dentalclinic.service;

import com.gayatri.dentalclinic.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.time.Clock;
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

    public void requireFuture(LocalDate date, LocalTime time) {
        if (date == null || time == null || !date.atTime(time).isAfter(now())) {
            throw new BadRequestException("Please choose a future appointment date and time.");
        }
    }
}
