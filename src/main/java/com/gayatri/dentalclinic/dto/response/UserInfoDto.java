package com.gayatri.dentalclinic.dto.response;

import com.gayatri.dentalclinic.enums.Role;
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
public class UserInfoDto {
    private Long id;
    private String email;
    private Role role;
    private Long patientId;
    private String name;
}
