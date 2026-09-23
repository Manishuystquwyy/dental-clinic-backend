package com.gayatri.dentalclinic.service;

import com.gayatri.dentalclinic.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class BookingTimeTest {
    @Test
    void usesClinicDateEvenWhenUtcIsStillYesterday() {
        BookingTime time = new BookingTime(Clock.fixed(Instant.parse("2026-09-23T19:00:00Z"), ZoneOffset.UTC));
        assertEquals(LocalDateTime.of(2026, 9, 24, 0, 30), time.now());
        assertThrows(BadRequestException.class,
                () -> time.requireFuture(LocalDate.of(2026, 9, 23), LocalTime.of(18, 0)));
        assertDoesNotThrow(() -> time.requireFuture(LocalDate.of(2026, 9, 24), LocalTime.of(10, 30)));
    }

    @Test
    void rejectsSlotAtItsStartAndAfterButAllowsItBefore() {
        LocalDate date = LocalDate.of(2026, 9, 24);
        LocalTime slot = LocalTime.of(14, 0);
        BookingTime before = new BookingTime(Clock.fixed(Instant.parse("2026-09-24T08:29:59Z"), ZoneOffset.UTC));
        BookingTime at = new BookingTime(Clock.fixed(Instant.parse("2026-09-24T08:30:00Z"), ZoneOffset.UTC));
        BookingTime after = new BookingTime(Clock.fixed(Instant.parse("2026-09-24T08:30:01Z"), ZoneOffset.UTC));
        assertDoesNotThrow(() -> before.requireFuture(date, slot));
        assertThrows(BadRequestException.class, () -> at.requireFuture(date, slot));
        assertThrows(BadRequestException.class, () -> after.requireFuture(date, slot));
    }
}
