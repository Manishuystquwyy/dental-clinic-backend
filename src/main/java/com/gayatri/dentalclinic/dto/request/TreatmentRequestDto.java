package com.gayatri.dentalclinic.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TreatmentRequestDto {

    @Schema(description = "Appointment id", example = "1")
    @NotNull(message = "Appointment id is required")
    private Long appointmentId;

    @Schema(description = "Treatment name", example = "Root Canal")
    @NotBlank(message = "Treatment name is required")
    private String treatmentName;

    @Schema(description = "Treatment description", example = "Root canal for molar tooth")
    private String description;

    @Schema(description = "Treatment cost", example = "2500.00")
    @NotNull(message = "Cost is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Cost must be 0 or greater")
    private BigDecimal cost;
}
