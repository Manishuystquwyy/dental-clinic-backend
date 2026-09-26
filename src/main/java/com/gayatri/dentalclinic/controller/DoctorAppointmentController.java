package com.gayatri.dentalclinic.controller;

import com.gayatri.dentalclinic.dto.response.AppointmentResponseDto;
import com.gayatri.dentalclinic.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/doctors/me")
@RequiredArgsConstructor
@Tag(name = "Doctor Appointments", description = "Appointments for the authenticated doctor")
@SecurityRequirement(name = "BearerAuth")
public class DoctorAppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping("/appointments")
    @Operation(
            summary = "Get the current doctor's appointments",
            description = "Uses the authenticated JWT account to return only appointments assigned to that doctor."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Doctor appointments",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = AppointmentResponseDto.class)))),
            @ApiResponse(responseCode = "403", description = "Caller is not a doctor", content = @Content),
            @ApiResponse(responseCode = "404", description = "Doctor profile was not found", content = @Content)
    })
    public List<AppointmentResponseDto> getMyAppointments() {
        return appointmentService.getCurrentDoctorAppointments();
    }
}
