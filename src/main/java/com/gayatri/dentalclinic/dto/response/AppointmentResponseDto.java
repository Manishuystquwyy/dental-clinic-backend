package com.gayatri.dentalclinic.dto.response;

import com.gayatri.dentalclinic.enums.AppointmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentResponseDto {

    @Schema(description = "Appointment id", example = "1")
    private Long id;

    @Schema(description = "Patient id", example = "1")
    private Long patientId;

    @Schema(description = "Dentist id", example = "1")
    private Long dentistId;

    @Schema(description = "Appointment date", example = "2026-02-10")
    private LocalDate appointmentDate;

    @Schema(description = "Appointment time", example = "10:30")
    private LocalTime appointmentTime;

    @Schema(description = "Appointment status", example = "BOOKED")
    private AppointmentStatus status;

    @Schema(description = "Notes or remarks", example = "Initial consultation")
    private String remarks;
}
