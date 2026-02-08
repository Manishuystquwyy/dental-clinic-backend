package com.gayatri.dentalclinic.controller;

import com.gayatri.dentalclinic.dto.request.TreatmentRequestDto;
import com.gayatri.dentalclinic.dto.response.TreatmentResponseDto;
import com.gayatri.dentalclinic.service.TreatmentService;
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
@RequestMapping("/api/treatments")
@RequiredArgsConstructor
@Tag(name = "Treatments", description = "Treatment management endpoints")
public class TreatmentController {

    private final TreatmentService treatmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a treatment", description = "Creates a new treatment record.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Treatment created",
                    content = @Content(schema = @Schema(implementation = TreatmentResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "404", description = "Appointment not found", content = @Content)
    })
    public TreatmentResponseDto createTreatment(@Valid @RequestBody TreatmentRequestDto requestDto) {
        return treatmentService.createTreatment(requestDto);
    }

    @GetMapping
    @Operation(summary = "List treatments", description = "Returns all treatments.")
    @ApiResponse(responseCode = "200", description = "Treatment list",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = TreatmentResponseDto.class))))
    public List<TreatmentResponseDto> getAllTreatments() {
        return treatmentService.getAllTreatments();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get treatment by id", description = "Returns a treatment by id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Treatment found",
                    content = @Content(schema = @Schema(implementation = TreatmentResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Treatment not found", content = @Content)
    })
    public TreatmentResponseDto getTreatmentById(
            @Parameter(description = "Treatment id", example = "1")
            @PathVariable Long id) {
        return treatmentService.getTreatmentById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update treatment", description = "Updates a treatment by id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Treatment updated",
                    content = @Content(schema = @Schema(implementation = TreatmentResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "404", description = "Treatment or appointment not found", content = @Content)
    })
    public TreatmentResponseDto updateTreatment(
            @Parameter(description = "Treatment id", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody TreatmentRequestDto requestDto) {
        return treatmentService.updateTreatment(id, requestDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete treatment", description = "Deletes a treatment by id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Treatment deleted"),
            @ApiResponse(responseCode = "404", description = "Treatment not found", content = @Content)
    })
    public void deleteTreatment(
            @Parameter(description = "Treatment id", example = "1")
            @PathVariable Long id) {
        treatmentService.deleteTreatment(id);
    }
}
