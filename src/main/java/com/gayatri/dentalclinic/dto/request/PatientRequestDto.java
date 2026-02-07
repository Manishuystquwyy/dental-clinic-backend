package com.gayatri.dentalclinic.dto.request;

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

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String gender;

    private LocalDate dateOfBirth;

    @NotBlank(message = "Phone number is required")
    @Size(min = 10, max = 15)
    private String phone;

    @Email(message = "Invalid email format")
    private String email;

    private String address;


}
