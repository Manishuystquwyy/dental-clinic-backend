package com.gayatri.dentalclinic.controller;

import com.gayatri.dentalclinic.dto.request.DentistRequestDto;
import com.gayatri.dentalclinic.dto.response.DentistResponseDto;
import com.gayatri.dentalclinic.service.FileStorageService;
import com.gayatri.dentalclinic.service.DentistService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/dentists")
@RequiredArgsConstructor
@Tag(name = "Dentists", description = "Dentist management endpoints")
public class DentistController {

    private final DentistService dentistService;
    private final FileStorageService fileStorageService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a dentist", description = "Creates a new dentist record.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Dentist created",
                    content = @Content(schema = @Schema(implementation = DentistResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content)
    })
    public DentistResponseDto createDentist(@Valid @RequestBody DentistRequestDto requestDto) {
        return dentistService.createDentist(requestDto);
    }

    @GetMapping
    @Operation(summary = "List dentists", description = "Returns all dentists.")
    @ApiResponse(responseCode = "200", description = "Dentist list",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = DentistResponseDto.class))))
    public List<DentistResponseDto> getAllDentists() {
        return dentistService.getAllDentists();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get dentist by id", description = "Returns a dentist by id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dentist found",
                    content = @Content(schema = @Schema(implementation = DentistResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Dentist not found", content = @Content)
    })
    public DentistResponseDto getDentistById(
            @Parameter(description = "Dentist id", example = "1")
            @PathVariable Long id) {
        return dentistService.getDentistById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update dentist", description = "Updates a dentist by id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dentist updated",
                    content = @Content(schema = @Schema(implementation = DentistResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "404", description = "Dentist not found", content = @Content)
    })
    public DentistResponseDto updateDentist(
            @Parameter(description = "Dentist id", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody DentistRequestDto requestDto) {
        return dentistService.updateDentist(id, requestDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete dentist", description = "Deletes a dentist by id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Dentist deleted"),
            @ApiResponse(responseCode = "404", description = "Dentist not found", content = @Content)
    })
    public void deleteDentist(
            @Parameter(description = "Dentist id", example = "1")
            @PathVariable Long id) {
        dentistService.deleteDentist(id);
    }

    @PostMapping(value = "/{id}/picture", consumes = {"multipart/form-data"})
    @Operation(summary = "Upload dentist picture", description = "Uploads a profile picture for a dentist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Picture uploaded",
                    content = @Content(schema = @Schema(implementation = DentistResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file", content = @Content),
            @ApiResponse(responseCode = "404", description = "Dentist not found", content = @Content)
    })
    public DentistResponseDto uploadPicture(
            @Parameter(description = "Dentist id", example = "1") @PathVariable Long id,
            @Parameter(description = "Image file") @RequestPart("file") MultipartFile file) {
        String url = fileStorageService.storeDentistPicture(id, file);
        return dentistService.updateDentistPicture(id, url);
    }
}
