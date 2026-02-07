package com.gayatri.dentalclinic.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PatientResponseDto {
    @Schema(description = "Patient id", example = "1")
    private Long id;
    @Schema(description = "Patient first name", example = "Ava")
    private String firstName;
    @Schema(description = "Patient last name", example = "Sharma")
    private String lastName;
    @Schema(description = "Patient gender", example = "Female")
    private String gender;
    @Schema(description = "Patient date of birth", example = "1994-06-12")
    private LocalDate dateOfBirth;
    @Schema(description = "Patient phone number", example = "9876543210")
    private String phone;
    @Schema(description = "Patient email address", example = "ava.sharma@example.com")
    private String email;
    @Schema(description = "Patient address", example = "12 MG Road, Pune")
    private String address;
    @Schema(description = "Created timestamp", example = "2026-02-08T03:12:00")
    private LocalDateTime createdAt;
}
