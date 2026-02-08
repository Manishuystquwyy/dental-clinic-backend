package com.gayatri.dentalclinic.controller;

import com.gayatri.dentalclinic.dto.request.AppointmentRequestDto;
import com.gayatri.dentalclinic.dto.response.AppointmentResponseDto;
import com.gayatri.dentalclinic.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Appointment management endpoints")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an appointment", description = "Creates a new appointment.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Appointment created",
                    content = @Content(schema = @Schema(implementation = AppointmentResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "404", description = "Patient or dentist not found", content = @Content)
    })
    public AppointmentResponseDto createAppointment(@Valid @RequestBody AppointmentRequestDto requestDto) {
        return appointmentService.createAppointment(requestDto);
    }

    @GetMapping
    @Operation(summary = "List appointments", description = "Returns all appointments.")
    @ApiResponse(responseCode = "200", description = "Appointment list",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AppointmentResponseDto.class))))
    public List<AppointmentResponseDto> getAllAppointments() {
        return appointmentService.getAllAppointments();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment by id", description = "Returns an appointment by id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Appointment found",
                    content = @Content(schema = @Schema(implementation = AppointmentResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Appointment not found", content = @Content)
    })
    public AppointmentResponseDto getAppointmentById(
            @Parameter(description = "Appointment id", example = "1")
            @PathVariable Long id) {
        return appointmentService.getAppointmentById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update appointment", description = "Updates an appointment by id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Appointment updated",
                    content = @Content(schema = @Schema(implementation = AppointmentResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "404", description = "Appointment, patient, or dentist not found", content = @Content)
    })
    public AppointmentResponseDto updateAppointment(
            @Parameter(description = "Appointment id", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody AppointmentRequestDto requestDto) {
        return appointmentService.updateAppointment(id, requestDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete appointment", description = "Deletes an appointment by id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Appointment deleted"),
            @ApiResponse(responseCode = "404", description = "Appointment not found", content = @Content)
    })
    public void deleteAppointment(
            @Parameter(description = "Appointment id", example = "1")
            @PathVariable Long id) {
        appointmentService.deleteAppointment(id);
    }
}
