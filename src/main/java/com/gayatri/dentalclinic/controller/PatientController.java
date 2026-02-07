package com.gayatri.dentalclinic.controller;

import com.gayatri.dentalclinic.dto.request.PatientRequestDto;
import com.gayatri.dentalclinic.dto.response.PatientResponseDto;
import com.gayatri.dentalclinic.service.PatientService;
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
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Tag(name = "Patients", description = "Patient management endpoints")
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a patient", description = "Creates a new patient record.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Patient created",
                    content = @Content(schema = @Schema(implementation = PatientResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content)
    })
    public PatientResponseDto createPatient(@Valid @RequestBody PatientRequestDto requestDto){
        return patientService.createPatient(requestDto);
    }

    @GetMapping
    @Operation(summary = "List patients", description = "Returns all patients.")
    @ApiResponse(responseCode = "200", description = "Patient list",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = PatientResponseDto.class))))
    public List<PatientResponseDto> getAllPatients() {
        return patientService.getAllPatients();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get patient by id", description = "Returns a patient by id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patient found",
                    content = @Content(schema = @Schema(implementation = PatientResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Patient not found", content = @Content)
    })
    public PatientResponseDto getPatientById(
            @Parameter(description = "Patient id", example = "1")
            @PathVariable Long id) {
        return patientService.getPatientById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update patient", description = "Updates a patient by id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Patient updated",
                    content = @Content(schema = @Schema(implementation = PatientResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "404", description = "Patient not found", content = @Content)
    })
    public PatientResponseDto updatePatient(
            @Parameter(description = "Patient id", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody PatientRequestDto requestDto) {
        return patientService.updatePatient(id, requestDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete patient", description = "Deletes a patient by id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Patient deleted"),
            @ApiResponse(responseCode = "404", description = "Patient not found", content = @Content)
    })
    public void deletePatient(
            @Parameter(description = "Patient id", example = "1")
            @PathVariable Long id) {
        patientService.deletePatient(id);
    }
}
