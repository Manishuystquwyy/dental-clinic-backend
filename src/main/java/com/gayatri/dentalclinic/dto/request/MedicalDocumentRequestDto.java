package com.gayatri.dentalclinic.dto.request;

import com.gayatri.dentalclinic.enums.MedicalRecordType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MedicalDocumentRequestDto {
    @NotBlank
    @Size(max = 160)
    private String title;
    @NotNull
    private MedicalRecordType type;
    @Size(max = 2000)
    private String notes;
}
