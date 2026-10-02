package com.gayatri.dentalclinic.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GoogleProfileDto(
        @NotBlank @Size(max = 100)
        @Pattern(regexp = "\\p{L}[\\p{L}\\p{M} '’\\-]*") String firstName,
        @NotBlank @Size(max = 100)
        @Pattern(regexp = "\\p{L}[\\p{L}\\p{M} '’\\-]*") String lastName,
        @NotBlank @Pattern(regexp = "[6-9][0-9]{9}", message = "Enter a valid 10-digit Indian mobile number") String phone,
        @NotBlank @Pattern(regexp = "Male|Female|Other|Prefer not to say") String gender) {
    public GoogleProfileDto {
        firstName = firstName == null ? null : firstName.trim();
        lastName = lastName == null ? null : lastName.trim();
        phone = phone == null ? null : phone.trim();
        gender = gender == null ? null : gender.trim();
    }
}
