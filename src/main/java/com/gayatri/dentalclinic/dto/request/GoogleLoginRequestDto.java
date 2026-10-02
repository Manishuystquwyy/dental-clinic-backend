package com.gayatri.dentalclinic.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleLoginRequestDto(
        @NotBlank @Size(max = 10000) String credential,
        @Valid GoogleProfileDto profile) {
}
