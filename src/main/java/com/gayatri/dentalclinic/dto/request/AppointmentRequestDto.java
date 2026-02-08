package com.gayatri.dentalclinic.dto.request;

import com.gayatri.dentalclinic.enums.AppointmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRequestDto {

    @Schema(description = "Patient id", example = "1")
    @NotNull(message = "Patient id is required")
    private Long patientId;

    @Schema(description = "Dentist id", example = "1")
    @NotNull(message = "Dentist id is required")
    private Long dentistId;

    @Schema(description = "Appointment date", example = "2026-02-10")
    @NotNull(message = "Appointment date is required")
    private LocalDate appointmentDate;

    @Schema(description = "Appointment time", example = "10:30")
    @NotNull(message = "Appointment time is required")
    private LocalTime appointmentTime;

    @Schema(description = "Appointment status", example = "BOOKED")
    private AppointmentStatus status;

    @Schema(description = "Notes or remarks", example = "Initial consultation")
    private String remarks;
}
