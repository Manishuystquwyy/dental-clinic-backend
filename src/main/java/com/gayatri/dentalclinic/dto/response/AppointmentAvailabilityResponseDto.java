package com.gayatri.dentalclinic.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentAvailabilityResponseDto {

    @Schema(description = "Dentist id", example = "1")
    private Long dentistId;

    @Schema(description = "Appointment date", example = "2026-09-20")
    private LocalDate appointmentDate;

    @Schema(description = "Times that can still be booked")
    private List<LocalTime> availableSlots;
}
