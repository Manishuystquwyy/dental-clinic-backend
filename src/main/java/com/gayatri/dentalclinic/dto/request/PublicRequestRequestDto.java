package com.gayatri.dentalclinic.dto.request;

import com.gayatri.dentalclinic.enums.PublicRequestType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PublicRequestRequestDto {

    @Schema(description = "Visitor name", example = "Rohit Sharma")
    @NotBlank(message = "Name is required")
    private String name;

    @Schema(description = "Visitor phone number", example = "9876543210")
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone number must be a valid 10-digit mobile number")
    private String phone;

    @Schema(description = "Visitor message", example = "I need to book a consultation this week.")
    @NotBlank(message = "Message is required")
    @Size(max = 2000, message = "Message must be 2000 characters or less")
    private String message;

    @Schema(description = "Request type", example = "CONTACT")
    @NotNull(message = "Request type is required")
    private PublicRequestType requestType;
}
