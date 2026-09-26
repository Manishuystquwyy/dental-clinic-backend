package com.gayatri.dentalclinic.controller;

import com.gayatri.dentalclinic.dto.request.DoctorRegistrationRequestDto;
import com.gayatri.dentalclinic.dto.response.DoctorRegistrationResponseDto;
import com.gayatri.dentalclinic.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/doctors")
@RequiredArgsConstructor
@Tag(name = "Admin Doctors", description = "Administrator-only doctor account management")
@SecurityRequirement(name = "BearerAuth")
public class AdminDoctorController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Register a doctor account",
            description = "Creates a DOCTOR user account and its linked dentist record. Requires an ADMIN JWT."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Doctor account created",
                    content = @Content(schema = @Schema(implementation = DoctorRegistrationResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or duplicate email", content = @Content),
            @ApiResponse(responseCode = "403", description = "Caller is not an administrator", content = @Content),
            @ApiResponse(responseCode = "409", description = "Duplicate value detected while saving", content = @Content)
    })
    public DoctorRegistrationResponseDto registerDoctor(
            @Valid @RequestBody DoctorRegistrationRequestDto requestDto) {
        return authService.registerDoctor(requestDto);
    }
}
