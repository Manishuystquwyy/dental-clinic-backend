package com.gayatri.dentalclinic.service;

import com.gayatri.dentalclinic.dto.request.*;
import com.gayatri.dentalclinic.entity.*;
import com.gayatri.dentalclinic.enums.Role;
import com.gayatri.dentalclinic.exception.*;
import com.gayatri.dentalclinic.repository.*;
import com.gayatri.dentalclinic.security.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleAuthServiceTest {
    private final GoogleTokenVerifier verifier = mock(GoogleTokenVerifier.class);
    private final UserAccountRepository accounts = mock(UserAccountRepository.class);
    private final PatientRepository patients = mock(PatientRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final JwtUtil jwt = mock(JwtUtil.class);
    private final LoginFraudDetectionService fraud = mock(LoginFraudDetectionService.class);
    private final GoogleAuthService service = new GoogleAuthService(verifier, accounts, patients, encoder, jwt, fraud);
    private final GoogleLoginRequestDto request = new GoogleLoginRequestDto("verified-token", null);

    @BeforeEach void setup() {
        when(verifier.verify("verified-token")).thenReturn(new GoogleTokenVerifier.Identity("sub", "user@gmail.com", "Ava", "Sharma", true));
    }

    @Test void newUserMustCompleteProfileBeforeGettingSession() {
        var result = service.login(request, "ip");
        assertTrue(result.registrationRequired());
        assertNull(result.token());
        verify(accounts, never()).save(any());
        verifyNoInteractions(jwt, patients);
    }

    @Test void createsPatientWithVerifiedEmailAndFixedRole() {
        when(patients.save(any())).thenAnswer(call -> { Patient p = call.getArgument(0); p.setId(1L); return p; });
        when(accounts.save(any())).thenAnswer(call -> { UserAccount a = call.getArgument(0); a.setId(2L); return a; });
        when(encoder.encode(anyString())).thenReturn("random-password-hash");
        when(jwt.generateToken(any())).thenReturn("app-token");
        var result = service.login(new GoogleLoginRequestDto("verified-token", new GoogleProfileDto("Ava", "Sharma", "9876543210", "Female")), "ip");
        assertFalse(result.registrationRequired());
        assertEquals("app-token", result.token());
        assertEquals(Role.PATIENT, result.user().getRole());
        assertEquals("user@gmail.com", result.user().getEmail());
        verify(accounts).save(argThat(a -> "sub".equals(a.getGoogleSubject()) && a.getPatient() != null));
    }

    private UserAccount account() {
        return UserAccount.builder().id(1L).email("user@gmail.com").role(Role.PATIENT).passwordHash("existing-hash").build();
    }

    @Test void linksExistingAuthoritativeEmailPreservingPasswordAndRole() {
        var account = account(); account.setRole(Role.DOCTOR);
        when(accounts.findByEmail("user@gmail.com")).thenReturn(Optional.of(account));
        when(accounts.save(account)).thenReturn(account);
        var result = service.login(request, "ip");
        assertEquals("sub", account.getGoogleSubject());
        assertEquals("existing-hash", account.getPasswordHash());
        assertEquals(Role.DOCTOR, result.user().getRole());
        verifyNoInteractions(patients, encoder);
    }

    @Test void rejectsUnsafeLinkAndConflictingGoogleSubject() {
        var account = account();
        when(accounts.findByEmail("user@gmail.com")).thenReturn(Optional.of(account));
        account.setGoogleSubject("other-sub");
        assertThrows(BadRequestException.class, () -> service.login(request, "ip"));
        account.setGoogleSubject(null);
        when(verifier.verify(anyString())).thenReturn(new GoogleTokenVerifier.Identity("sub", "user@gmail.com", "Ava", "Sharma", false));
        assertThrows(BadRequestException.class, () -> service.login(request, "ip"));
        verifyNoInteractions(jwt);
        verify(accounts, never()).save(any());
    }

    @Test void returningUserIsIdentifiedBySubjectEvenIfGoogleEmailChanges() {
        var account = account(); account.setEmail("original@gmail.com"); account.setGoogleSubject("sub");
        when(accounts.findByGoogleSubject("sub")).thenReturn(Optional.of(account));
        assertEquals("original@gmail.com", service.login(request, "ip").user().getEmail());
        verify(accounts, never()).findByEmail(anyString());
        verify(fraud).checkLoginAllowed("original@gmail.com", "ip");
    }

    @Test void blockedAccountCannotUseGoogleToBypassBlock() {
        var account = account(); account.setLoginBlocked(true);
        when(accounts.findByGoogleSubject("sub")).thenReturn(Optional.of(account));
        assertThrows(BadRequestException.class, () -> service.login(request, "ip"));
        verifyNoInteractions(jwt);
    }

    @Test void duplicatePhoneDoesNotCreatePatientOrSession() {
        when(patients.existsByPhone("9876543210")).thenReturn(true);
        assertThrows(SignupValidationException.class, () -> service.login(new GoogleLoginRequestDto("verified-token", new GoogleProfileDto("Ava", "Sharma", "9876543210", "Female")), "ip"));
        verify(patients, never()).save(any());
        verify(accounts, never()).save(any());
        verifyNoInteractions(jwt);
    }
}
