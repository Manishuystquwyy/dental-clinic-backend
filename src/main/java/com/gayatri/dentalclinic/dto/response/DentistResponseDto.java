package com.gayatri.dentalclinic.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DentistResponseDto {

    @Schema(description = "Dentist id", example = "1")
    private Long id;

    @Schema(description = "Dentist full name", example = "Dr. Riya Kapoor")
    private String name;

    @Schema(description = "Dentist phone number", example = "9876543210")
    private String phone;

    @Schema(description = "Dentist email address", example = "riya.kapoor@example.com")
    private String email;

    @Schema(description = "Years of experience", example = "8")
    private int experienceYears;

    @Schema(description = "Highest qualification", example = "BDS")
    private String qualification;

    @Schema(description = "Specialization", example = "Orthodontist")
    private String specialization;

    @Schema(description = "Profile image URL", example = "https://example.com/images/dentists/riya.jpg")
    private String pictureUrl;
}
