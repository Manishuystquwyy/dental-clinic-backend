package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.dto.request.AuthLoginRequestDto;
import com.gayatri.dentalclinic.dto.request.AuthRegisterRequestDto;
import com.gayatri.dentalclinic.dto.request.PatientRequestDto;
import com.gayatri.dentalclinic.dto.response.AuthResponseDto;
import com.gayatri.dentalclinic.dto.response.UserInfoDto;
import com.gayatri.dentalclinic.entity.Patient;
import com.gayatri.dentalclinic.entity.UserAccount;
import com.gayatri.dentalclinic.enums.Role;
import com.gayatri.dentalclinic.exception.BadRequestException;
import com.gayatri.dentalclinic.mapper.PatientMapper;
import com.gayatri.dentalclinic.repository.PatientRepository;
import com.gayatri.dentalclinic.repository.UserAccountRepository;
import com.gayatri.dentalclinic.security.CustomUserDetails;
import com.gayatri.dentalclinic.security.JwtUtil;
import com.gayatri.dentalclinic.security.SecurityUtils;
import com.gayatri.dentalclinic.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public AuthResponseDto registerPatient(AuthRegisterRequestDto requestDto) {
        if (userAccountRepository.existsByEmail(requestDto.getEmail())) {
            throw new BadRequestException("Email already exists");
        }
        if (patientRepository.existsByPhone(requestDto.getPhone())) {
            throw new BadRequestException("Phone number already exists");
        }
        if (patientRepository.existsByEmail(requestDto.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        PatientRequestDto patientRequest = new PatientRequestDto(
                requestDto.getFirstName(),
                requestDto.getLastName(),
                requestDto.getGender(),
                requestDto.getDateOfBirth(),
                requestDto.getPhone(),
                requestDto.getEmail(),
                requestDto.getAddress()
        );

        Patient patient = PatientMapper.toEntity(patientRequest);
        Patient savedPatient = patientRepository.save(patient);

        UserAccount account = UserAccount.builder()
                .email(requestDto.getEmail())
                .passwordHash(passwordEncoder.encode(requestDto.getPassword()))
                .role(Role.PATIENT)
                .patient(savedPatient)
                .build();

        UserAccount savedAccount = userAccountRepository.save(account);
        CustomUserDetails userDetails = CustomUserDetails.fromUserAccount(savedAccount);

        return AuthResponseDto.builder()
                .token(jwtUtil.generateToken(userDetails))
                .user(toUserInfo(savedAccount))
                .build();
    }

    @Override
    public AuthResponseDto login(AuthLoginRequestDto requestDto) {
        UserAccount account = userAccountRepository.findByEmail(requestDto.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(requestDto.getPassword(), account.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }

        CustomUserDetails userDetails = CustomUserDetails.fromUserAccount(account);
        return AuthResponseDto.builder()
                .token(jwtUtil.generateToken(userDetails))
                .user(toUserInfo(account))
                .build();
    }

    @Override
    public UserInfoDto getCurrentUser() {
        CustomUserDetails current = SecurityUtils.getCurrentUser();
        if (current == null) {
            return null;
        }
        UserAccount account = userAccountRepository.findById(current.getId()).orElse(null);
        return account != null ? toUserInfo(account) : null;
    }

    private UserInfoDto toUserInfo(UserAccount account) {
        String name = null;
        Long patientId = null;
        if (account.getPatient() != null) {
            patientId = account.getPatient().getId();
            name = (account.getPatient().getFirstName() + " " + account.getPatient().getLastName()).trim();
        }
        return UserInfoDto.builder()
                .id(account.getId())
                .email(account.getEmail())
                .role(account.getRole())
                .patientId(patientId)
                .name(name)
                .build();
    }
}
