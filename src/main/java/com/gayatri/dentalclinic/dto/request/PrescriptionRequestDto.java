package com.gayatri.dentalclinic.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PrescriptionRequestDto(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 12000) String prescriptionText,
        @Size(max = 2000) String notes
) {
}
