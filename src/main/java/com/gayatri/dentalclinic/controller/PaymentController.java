package com.gayatri.dentalclinic.controller;

import com.gayatri.dentalclinic.dto.request.PaymentRequestDto;
import com.gayatri.dentalclinic.dto.response.PaymentResponseDto;
import com.gayatri.dentalclinic.service.PaymentService;
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
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment management endpoints")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a payment", description = "Creates a new payment record.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Payment created",
                    content = @Content(schema = @Schema(implementation = PaymentResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "404", description = "Bill not found", content = @Content)
    })
    public PaymentResponseDto createPayment(@Valid @RequestBody PaymentRequestDto requestDto) {
        return paymentService.createPayment(requestDto);
    }

    @GetMapping
    @Operation(summary = "List payments", description = "Returns all payments.")
    @ApiResponse(responseCode = "200", description = "Payment list",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = PaymentResponseDto.class))))
    public List<PaymentResponseDto> getAllPayments() {
        return paymentService.getAllPayments();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by id", description = "Returns a payment by id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment found",
                    content = @Content(schema = @Schema(implementation = PaymentResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Payment not found", content = @Content)
    })
    public PaymentResponseDto getPaymentById(
            @Parameter(description = "Payment id", example = "1")
            @PathVariable Long id) {
        return paymentService.getPaymentById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update payment", description = "Updates a payment by id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment updated",
                    content = @Content(schema = @Schema(implementation = PaymentResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "404", description = "Payment or bill not found", content = @Content)
    })
    public PaymentResponseDto updatePayment(
            @Parameter(description = "Payment id", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody PaymentRequestDto requestDto) {
        return paymentService.updatePayment(id, requestDto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete payment", description = "Deletes a payment by id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Payment deleted"),
            @ApiResponse(responseCode = "404", description = "Payment not found", content = @Content)
    })
    public void deletePayment(
            @Parameter(description = "Payment id", example = "1")
            @PathVariable Long id) {
        paymentService.deletePayment(id);
    }
}
