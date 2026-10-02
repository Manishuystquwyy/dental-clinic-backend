package com.gayatri.dentalclinic.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordRequestDto {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    public void setEmail(String value) {
        email = value == null ? null : value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
