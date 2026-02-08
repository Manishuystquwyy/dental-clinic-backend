package com.gayatri.dentalclinic.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TreatmentResponseDto {

    @Schema(description = "Treatment id", example = "1")
    private Long id;

    @Schema(description = "Appointment id", example = "1")
    private Long appointmentId;

    @Schema(description = "Treatment name", example = "Root Canal")
    private String treatmentName;

    @Schema(description = "Treatment description", example = "Root canal for molar tooth")
    private String description;

    @Schema(description = "Treatment cost", example = "2500.00")
    private BigDecimal cost;
}
