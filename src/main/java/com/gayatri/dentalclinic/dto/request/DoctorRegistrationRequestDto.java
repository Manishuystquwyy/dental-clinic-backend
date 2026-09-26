package com.gayatri.dentalclinic.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DoctorRegistrationRequestDto {

    @Schema(description = "Doctor full name", example = "Dr. Riya Kapoor")
    @NotBlank(message = "Full name is required")
    private String fullName;

    @Schema(description = "Doctor email address", example = "riya.kapoor@example.com")
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @Schema(description = "Doctor phone number", example = "9876543210")
    @NotBlank(message = "Phone number is required")
    @Size(min = 10, max = 15, message = "Phone number must be between 10 and 15 characters")
    private String phone;

    @Schema(description = "Initial account password", example = "StrongPass@123", accessMode = Schema.AccessMode.WRITE_ONLY)
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    @Schema(description = "Doctor specialization", example = "Orthodontist")
    @NotBlank(message = "Specialization is required")
    private String specialization;

    @Schema(description = "Highest qualification", example = "BDS")
    @NotBlank(message = "Qualification is required")
    private String qualification;

    @Schema(description = "Years of experience", example = "8")
    @NotNull(message = "Experience is required")
    @Min(value = 0, message = "Experience must be 0 or greater")
    private Integer experienceYears;

    @Schema(description = "Consultation fee", example = "500.00")
    @NotNull(message = "Consultation fee is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Consultation fee must be 0 or greater")
    private BigDecimal consultationFee;
}
