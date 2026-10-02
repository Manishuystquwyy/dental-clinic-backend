package com.gayatri.dentalclinic.service;

import com.gayatri.dentalclinic.dto.request.AuthLoginRequestDto;
import com.gayatri.dentalclinic.dto.request.AuthRegisterRequestDto;
import com.gayatri.dentalclinic.dto.request.DoctorRegistrationRequestDto;
import com.gayatri.dentalclinic.dto.response.AuthResponseDto;
import com.gayatri.dentalclinic.dto.response.DoctorRegistrationResponseDto;
import com.gayatri.dentalclinic.dto.response.UserInfoDto;

public interface AuthService {
    AuthResponseDto registerPatient(AuthRegisterRequestDto requestDto);
    DoctorRegistrationResponseDto registerDoctor(DoctorRegistrationRequestDto requestDto);
    AuthResponseDto login(AuthLoginRequestDto requestDto, String ipAddress);
    UserInfoDto getCurrentUser();
    void forgotPassword(String email);
    void resetPassword(String token, String newPassword);
}
