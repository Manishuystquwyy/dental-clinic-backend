package com.gayatri.dentalclinic.dto.response;

import com.gayatri.dentalclinic.enums.Role;
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
public class DoctorRegistrationResponseDto {

    @Schema(example = "Doctor account created successfully")
    private String message;

    @Schema(description = "User account id", example = "12")
    private Long userId;

    @Schema(description = "Doctor record id", example = "5")
    private Long doctorId;

    @Schema(example = "riya.kapoor@example.com")
    private String email;

    @Schema(example = "DOCTOR")
    private Role role;
}
