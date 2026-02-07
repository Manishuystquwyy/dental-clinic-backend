package com.gayatri.dentalclinic.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PatientRequestDto {

    @Schema(description = "Patient first name", example = "Ava")
    @NotBlank(message = "First name is required")
    private String firstName;

    @Schema(description = "Patient last name", example = "Sharma")
    @NotBlank(message = "Last name is required")
    private String lastName;

    @Schema(description = "Patient gender", example = "Female")
    private String gender;

    @Schema(description = "Patient date of birth", example = "1994-06-12")
    private LocalDate dateOfBirth;

    @Schema(description = "Patient phone number", example = "9876543210")
    @NotBlank(message = "Phone number is required")
    @Size(min = 10, max = 15)
    private String phone;

    @Schema(description = "Patient email address", example = "ava.sharma@example.com")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "Patient address", example = "12 MG Road, Pune")
    private String address;


}
