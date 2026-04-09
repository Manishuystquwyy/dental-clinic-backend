package com.gayatri.dentalclinic.dto.response;

import com.gayatri.dentalclinic.enums.PublicRequestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicRequestResponseDto {

    private Long id;
    private String name;
    private String phone;
    private String message;
    private PublicRequestType requestType;
    private LocalDateTime createdAt;
}
