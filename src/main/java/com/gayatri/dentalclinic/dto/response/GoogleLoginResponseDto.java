package com.gayatri.dentalclinic.dto.response;

public record GoogleLoginResponseDto(String token, UserInfoDto user, boolean registrationRequired,
                                     String email, String firstName, String lastName) {
}
