package com.gayatri.dentalclinic.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillResponseDto {

    @Schema(description = "Bill id", example = "1")
    private Long id;

    @Schema(description = "Appointment id", example = "1")
    private Long appointmentId;

    @Schema(description = "Total amount before discount", example = "3000.00")
    private BigDecimal totalAmount;

    @Schema(description = "Discount amount", example = "500.00")
    private BigDecimal discount;

    @Schema(description = "Final amount after discount", example = "2500.00")
    private BigDecimal finalAmount;

    @Schema(description = "Bill date", example = "2026-02-10")
    private LocalDate billDate;
}
