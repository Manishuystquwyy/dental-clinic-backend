package com.gayatri.dentalclinic.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BillRequestDto {

    @Schema(description = "Appointment id", example = "1")
    @NotNull(message = "Appointment id is required")
    private Long appointmentId;

    @Schema(description = "Total amount before discount", example = "3000.00")
    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Total amount must be 0 or greater")
    private BigDecimal totalAmount;

    @Schema(description = "Discount amount", example = "500.00")
    @NotNull(message = "Discount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Discount must be 0 or greater")
    private BigDecimal discount;

    @Schema(description = "Final amount after discount", example = "2500.00")
    @NotNull(message = "Final amount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Final amount must be 0 or greater")
    private BigDecimal finalAmount;

    @Schema(description = "Bill date", example = "2026-02-10")
    @NotNull(message = "Bill date is required")
    private LocalDate billDate;
}
