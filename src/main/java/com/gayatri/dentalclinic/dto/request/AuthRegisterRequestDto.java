package com.gayatri.dentalclinic.dto.request;

import com.gayatri.dentalclinic.validation.ValidAddress;
import com.gayatri.dentalclinic.validation.AddressValidator;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.AssertTrue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class AuthRegisterRequestDto {

    @Schema(description = "Patient first name", example = "Ava")
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must be at most 100 characters")
    @Pattern(regexp = "\\p{L}[\\p{L}\\p{M} '’\\-]*", message = "First name must contain only letters, spaces, apostrophes, or hyphens")
    private String firstName;

    @Schema(description = "Patient last name", example = "Sharma")
    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must be at most 100 characters")
    @Pattern(regexp = "\\p{L}[\\p{L}\\p{M} '’\\-]*", message = "Last name must contain only letters, spaces, apostrophes, or hyphens")
    private String lastName;

    @Schema(description = "Patient gender", example = "Female")
    @Pattern(regexp = "Male|Female|Other|Prefer not to say", message = "Choose a valid gender option")
    @NotBlank(message = "Gender is required")
    private String gender;

    @Schema(description = "Patient date of birth", example = "1994-06-12")
    @PastOrPresent(message = "Date of birth cannot be in the future")
    private LocalDate dateOfBirth;

    @Schema(description = "Patient phone number", example = "9876543210")
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "[6-9][0-9]{9}", message = "Enter a 10-digit Indian mobile number starting with 6, 7, 8, or 9")
    private String phone;

    @Schema(description = "Patient email address", example = "ava.sharma@example.com")
    @Email(regexp = "^[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+$", message = "Invalid email format")
    @NotBlank(message = "Email is required")
    @Size(max = 254, message = "Email must be at most 254 characters")
    private String email;

    @Schema(description = "Patient address", example = "12 MG Road, Pune")
    @ValidAddress
    private String address;

    @Schema(description = "Account password", example = "StrongPass@123")
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    public AuthRegisterRequestDto(String firstName, String lastName, String gender, LocalDate dateOfBirth,
                                  String phone, String email, String address, String password) {
        setFirstName(firstName);
        setLastName(lastName);
        setGender(gender);
        this.dateOfBirth = dateOfBirth;
        setPhone(phone);
        setEmail(email);
        setAddress(address);
        this.password = password;
    }

    public void setFirstName(String value) { firstName = trim(value); }
    public void setLastName(String value) { lastName = trim(value); }
    public void setEmail(String value) { email = value == null ? null : value.trim().toLowerCase(Locale.ROOT); }
    public void setPhone(String value) { phone = trim(value); }
    public void setGender(String value) { gender = optional(value); }
    public void setAddress(String value) { address = AddressValidator.normalize(value); }

    @JsonIgnore
    @AssertTrue(message = "Password must be at most 72 UTF-8 bytes")
    public boolean isPasswordWithinByteLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    @JsonIgnore
    @AssertTrue(message = "Age cannot exceed 100 years")
    public boolean isDateOfBirthWithinAgeLimit() {
        return dateOfBirth == null || !dateOfBirth.isBefore(LocalDate.now().minusYears(100));
    }

    private static String trim(String value) { return value == null ? null : value.trim(); }
    private static String optional(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }
}
