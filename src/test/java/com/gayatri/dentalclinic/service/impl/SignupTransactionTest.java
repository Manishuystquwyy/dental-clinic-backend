package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.dto.request.AuthRegisterRequestDto;
import com.gayatri.dentalclinic.entity.Patient;
import com.gayatri.dentalclinic.entity.UserAccount;
import com.gayatri.dentalclinic.exception.SignupValidationException;
import com.gayatri.dentalclinic.repository.*;
import com.gayatri.dentalclinic.security.JwtUtil;
import com.gayatri.dentalclinic.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SignupTransactionTest {
    private final UserAccountRepository accounts = mock(UserAccountRepository.class);
    private final PatientRepository patients = mock(PatientRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final JwtUtil jwt = mock(JwtUtil.class);
    private final AuthServiceImpl service = new AuthServiceImpl(accounts, patients, mock(DentistRepository.class),
            encoder, jwt, mock(NotificationService.class), mock(LoginFraudDetectionService.class));

    private AuthRegisterRequestDto request() {
        return new AuthRegisterRequestDto("Ava", "Sharma", null, null, "9876543210", "ava@example.com", null, "password123");
    }

    @Test void accountFailureRollsBackPatientInsert() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:signupRollback;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("create table if not exists signup_patients (email varchar(254))");
        jdbc.update("delete from signup_patients");
        when(patients.save(any(Patient.class))).thenAnswer(invocation -> {
            Patient patient = invocation.getArgument(0);
            jdbc.update("insert into signup_patients(email) values (?)", patient.getEmail());
            return patient;
        });
        when(encoder.encode(anyString())).thenReturn("hash");
        when(accounts.save(any(UserAccount.class))).thenThrow(new IllegalStateException("Account save failed"));
        var proxy = new ProxyFactory(service);
        proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(dataSource), new AnnotationTransactionAttributeSource()));
        var transactionalService = (AuthService) proxy.getProxy();
        assertThrows(IllegalStateException.class, () -> transactionalService.registerPatient(request()));
        assertEquals(0, jdbc.queryForObject("select count(*) from signup_patients", Integer.class));
        verify(patients).save(any(Patient.class));
        verifyNoInteractions(jwt);
    }

    @Test void duplicateEmailReturnsFieldBeforeSaving() {
        when(accounts.existsByEmail("ava@example.com")).thenReturn(true);
        var error = assertThrows(SignupValidationException.class, () -> service.registerPatient(request()));
        assertEquals("email", error.getField());
        verify(patients, never()).save(any());
        verify(accounts, never()).save(any());
    }

    @Test void duplicatePhoneReturnsFieldBeforeSaving() {
        when(patients.existsByPhone("9876543210")).thenReturn(true);
        var error = assertThrows(SignupValidationException.class, () -> service.registerPatient(request()));
        assertEquals("phone", error.getField());
        verify(patients, never()).save(any());
        verify(accounts, never()).save(any());
    }
}
