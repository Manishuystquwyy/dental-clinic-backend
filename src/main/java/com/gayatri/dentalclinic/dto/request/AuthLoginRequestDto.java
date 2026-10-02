package com.gayatri.dentalclinic.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AuthLoginRequestDto {

    @Schema(description = "Account email", example = "ava.sharma@example.com")
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @Schema(description = "Account password", example = "StrongPass@123")
    @NotBlank(message = "Password is required")
    private String password;
    public AuthLoginRequestDto(String email, String password) {
        setEmail(email);
        this.password = password;
    }

    public void setEmail(String value) {
        email = value == null ? null : value.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
