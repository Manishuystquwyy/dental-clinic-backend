package com.gayatri.dentalclinic.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DentistRequestDto {

    @Schema(description = "Dentist full name", example = "Dr. Riya Kapoor")
    @NotBlank(message = "Name is required")
    private String name;

    @Schema(description = "Dentist phone number", example = "9876543210")
    @NotBlank(message = "Phone number is required")
    @Size(min = 10, max = 15)
    private String phone;

    @Schema(description = "Dentist email address", example = "riya.kapoor@example.com")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "Years of experience", example = "8")
    @Min(value = 0, message = "Experience years must be 0 or greater")
    private int experienceYears;

    @Schema(description = "Highest qualification", example = "BDS")
    private String qualification;

    @Schema(description = "Specialization", example = "Orthodontist")
    private String specialization;
}
