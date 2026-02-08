package com.gayatri.dentalclinic.service;

import com.gayatri.dentalclinic.dto.request.AuthLoginRequestDto;
import com.gayatri.dentalclinic.dto.request.AuthRegisterRequestDto;
import com.gayatri.dentalclinic.dto.response.AuthResponseDto;
import com.gayatri.dentalclinic.dto.response.UserInfoDto;

public interface AuthService {
    AuthResponseDto registerPatient(AuthRegisterRequestDto requestDto);
    AuthResponseDto login(AuthLoginRequestDto requestDto);
    UserInfoDto getCurrentUser();
}
