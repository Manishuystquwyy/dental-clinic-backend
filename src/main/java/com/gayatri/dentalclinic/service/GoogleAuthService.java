package com.gayatri.dentalclinic.service;

import com.gayatri.dentalclinic.dto.request.GoogleLoginRequestDto;
import com.gayatri.dentalclinic.dto.response.GoogleLoginResponseDto;
import com.gayatri.dentalclinic.dto.response.UserInfoDto;
import com.gayatri.dentalclinic.entity.Patient;
import com.gayatri.dentalclinic.entity.UserAccount;
import com.gayatri.dentalclinic.enums.Role;
import com.gayatri.dentalclinic.exception.BadRequestException;
import com.gayatri.dentalclinic.exception.SignupValidationException;
import com.gayatri.dentalclinic.repository.PatientRepository;
import com.gayatri.dentalclinic.repository.UserAccountRepository;
import com.gayatri.dentalclinic.security.CustomUserDetails;
import com.gayatri.dentalclinic.security.GoogleTokenVerifier;
import com.gayatri.dentalclinic.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {
    private final GoogleTokenVerifier verifier;
    private final UserAccountRepository accounts;
    private final PatientRepository patients;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginFraudDetectionService fraudDetection;

    @Transactional
    public GoogleLoginResponseDto login(GoogleLoginRequestDto request, String ipAddress) {
        var identity = verifier.verify(request.credential());
        UserAccount account = accounts.findByGoogleSubject(identity.subject()).orElse(null);
        fraudDetection.checkLoginAllowed(account == null ? identity.email() : account.getEmail(), ipAddress);
        if (account == null) {
            account = accounts.findByEmail(identity.email()).orElse(null);
            if (account != null) {
                if (!identity.authoritativeEmail() || (account.getGoogleSubject() != null
                        && !account.getGoogleSubject().equals(identity.subject()))) {
                    throw new BadRequestException("Please sign in with your existing email and password for this account.");
                }
                account.setGoogleSubject(identity.subject());
                account = accounts.save(account);
            } else {
                if (request.profile() == null) {
                    return new GoogleLoginResponseDto(null, null, true, identity.email(), identity.firstName(), identity.lastName());
                }
                var profile = request.profile();
                if (patients.existsByEmail(identity.email())) {
                    throw new SignupValidationException("email", "A patient record already uses this email. Please contact the clinic.");
                }
                if (patients.existsByPhone(profile.phone())) {
                    throw new SignupValidationException("phone", "Phone number already exists");
                }
                Patient patient = patients.save(Patient.builder()
                        .firstName(profile.firstName().trim()).lastName(profile.lastName().trim())
                        .phone(profile.phone()).gender(profile.gender()).email(identity.email())
                        .createdAt(LocalDateTime.now()).build());
                account = accounts.save(UserAccount.builder().email(identity.email())
                        .googleSubject(identity.subject()).role(Role.PATIENT).patient(patient)
                        // No usable password is exposed; password reset can establish one later.
                        .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString())).build());
            }
        }
        if (account.isLoginBlocked()) {
            throw new BadRequestException("Account is blocked. Please reset the password.");
        }
        fraudDetection.recordSuccessfulLogin(account.getEmail(), ipAddress);
        Patient patient = account.getPatient();
        UserInfoDto user = UserInfoDto.builder().id(account.getId()).email(account.getEmail()).role(account.getRole())
                .patientId(patient == null ? null : patient.getId())
                .name(patient == null ? null : (patient.getFirstName() + " " + patient.getLastName()).trim()).build();
        return new GoogleLoginResponseDto(jwtUtil.generateToken(CustomUserDetails.fromUserAccount(account)),
                user, false, null, null, null);
    }
}
