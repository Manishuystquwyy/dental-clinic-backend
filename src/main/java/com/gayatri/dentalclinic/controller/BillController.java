package com.gayatri.dentalclinic.controller;

import com.gayatri.dentalclinic.dto.request.BillRequestDto;
import com.gayatri.dentalclinic.dto.response.BillResponseDto;
import com.gayatri.dentalclinic.service.BillService;
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
@RequestMapping("/api/bills")
@RequiredArgsConstructor
@Tag(name = "Bills", description = "Billing management endpoints")
public class BillController {

    private final BillService billService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a bill", description = "Creates a new bill record.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Bill created",
                    content = @Content(schema = @Schema(implementation = BillResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "404", description = "Appointment not found", content = @Content)
    })
    public BillResponseDto createBill(@Valid @RequestBody BillRequestDto requestDto) {
        return billService.createBill(requestDto);
    }

    @GetMapping
    @Operation(summary = "List bills", description = "Returns all bills.")
    @ApiResponse(responseCode = "200", description = "Bill list",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = BillResponseDto.class))))
    public List<BillResponseDto> getAllBills() {
        return billService.getAllBills();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get bill by id", description = "Returns a bill by id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bill found",
                    content = @Content(schema = @Schema(implementation = BillResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Bill not found", content = @Content)
    })
    public BillResponseDto getBillById(
            @Parameter(description = "Bill id", example = "1")
            @PathVariable Long id) {
        return billService.getBillById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update bill", description = "Updates a bill by id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bill updated",
                    content = @Content(schema = @Schema(implementation = BillResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "404", description = "Bill or appointment not found", content = @Content)
    })
    public BillResponseDto updateBill(
            @Parameter(description = "Bill id", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody BillRequestDto requestDto) {
        return billService.updateBill(id, requestDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete bill", description = "Deletes a bill by id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Bill deleted"),
            @ApiResponse(responseCode = "404", description = "Bill not found", content = @Content)
    })
    public void deleteBill(
            @Parameter(description = "Bill id", example = "1")
            @PathVariable Long id) {
        billService.deleteBill(id);
    }
}
