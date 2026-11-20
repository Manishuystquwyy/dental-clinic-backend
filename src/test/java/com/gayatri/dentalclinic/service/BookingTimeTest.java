package com.gayatri.dentalclinic.service;

import com.gayatri.dentalclinic.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class BookingTimeTest {
    @Test
    void paymentDateUsesIndianMidnightRegardlessOfProcessingDay() {
        BookingTime time = new BookingTime(Clock.fixed(Instant.parse("2026-09-27T00:00:00Z"), ZoneOffset.UTC));
        assertEquals(LocalDate.of(2026, 9, 24), time.dateAt(Instant.parse("2026-09-24T18:29:59Z")));
        assertEquals(LocalDate.of(2026, 9, 25), time.dateAt(Instant.parse("2026-09-24T18:30:00Z")));
        assertEquals(LocalDate.of(2026, 9, 25), time.dateAt(Instant.parse("2026-09-24T19:59:53Z")));
    }
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

    @ParameterizedTest
    @CsvSource({
            "2026-09-24, 18:00:30, 2026-09-24T00:30:29Z, true",
            "2026-09-24, 18:00:30, 2026-09-24T00:30:30Z, true",
            "2026-09-24, 18:00:30, 2026-09-24T00:30:31Z, false",
            "2026-09-25, 10:30:00, 2026-09-24T16:59:59Z, true",
            "2026-09-25, 10:30:00, 2026-09-24T17:00:00Z, true",
            "2026-09-25, 10:30:00, 2026-09-24T17:00:00.001Z, false",
            "2026-09-25, 10:30:00, 2026-09-24T17:00:01Z, false",
            "2026-09-23, 10:30:00, 2026-09-24T08:30:00Z, false"
    })
    void onlineChangesUseTheActualStartTimeAndAnInclusiveTwelveHourCutoff(
            LocalDate appointmentDate, LocalTime appointmentTime, String instant, boolean allowed) {
        BookingTime time = new BookingTime(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));

        if (allowed) {
            assertDoesNotThrow(() -> time.requireOnlineChangeAllowed(appointmentDate, appointmentTime, 12, "rescheduled"));
        } else {
            assertThrows(BadRequestException.class,
                    () -> time.requireOnlineChangeAllowed(appointmentDate, appointmentTime, 12, "rescheduled"));
        }
    }

    @ParameterizedTest
    @CsvSource({
            "24, cancelled, 2026-09-24T04:59:59Z, true",
            "24, cancelled, 2026-09-24T05:00:00Z, true",
            "24, cancelled, 2026-09-24T05:00:00.001Z, false",
            "6, rescheduled, 2026-09-24T22:59:59Z, true",
            "6, rescheduled, 2026-09-24T23:00:00Z, true",
            "6, rescheduled, 2026-09-24T23:00:00.001Z, false",
            "0, cancelled, 2026-09-25T04:59:59Z, true",
            "0, cancelled, 2026-09-25T05:00:00Z, true",
            "0, cancelled, 2026-09-25T05:00:00.001Z, false"
    })
    void onlineChangesHonorTheConfiguredHoursIncludingZeroAndUseDynamicErrors(
            int cutoffHours, String action, String instant, boolean allowed) {
        BookingTime time = new BookingTime(Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
        var appointmentDate = LocalDate.of(2026, 9, 25);
        var appointmentTime = LocalTime.of(10, 30);

        if (allowed) {
            assertDoesNotThrow(() -> time.requireOnlineChangeAllowed(appointmentDate, appointmentTime, cutoffHours, action));
        } else {
            var error = assertThrows(BadRequestException.class,
                    () -> time.requireOnlineChangeAllowed(appointmentDate, appointmentTime, cutoffHours, action));
            assertEquals("Appointments can only be " + action + " at least " + cutoffHours
                    + " hours before their scheduled start time.", error.getMessage());
        }
    }
}
