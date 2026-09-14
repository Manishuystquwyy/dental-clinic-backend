package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.entity.UserAccount;
import com.gayatri.dentalclinic.exception.BadRequestException;
import com.gayatri.dentalclinic.exception.TooManyRequestsException;
import com.gayatri.dentalclinic.repository.LoginAttemptRepository;
import com.gayatri.dentalclinic.repository.UserAccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoginFraudDetectionServiceImplTest {

    private LoginAttemptRepository loginAttemptRepository;
    private UserAccountRepository userAccountRepository;
    private HttpServletRequest request;
    private LoginFraudDetectionServiceImpl service;

    @BeforeEach
    void setUp() {
        loginAttemptRepository = mock(LoginAttemptRepository.class);
        userAccountRepository = mock(UserAccountRepository.class);
        request = mock(HttpServletRequest.class);
        service = new LoginFraudDetectionServiceImpl(loginAttemptRepository, userAccountRepository);

        ReflectionTestUtils.setField(service, "maxFailedAttempts", 5);
        ReflectionTestUtils.setField(service, "failedAttemptWindowMinutes", 15L);
        ReflectionTestUtils.setField(service, "ipRateLimitAttempts", 10L);
        ReflectionTestUtils.setField(service, "ipRateLimitWindowMinutes", 10L);

        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Test
    void checkLoginAllowedBlocksAfterIpRateLimit() {
        when(loginAttemptRepository.countByIpAddressAndAttemptedAtGreaterThanEqual(
                eq("127.0.0.1"),
                any(LocalDateTime.class)
        )).thenReturn(10L);

        TooManyRequestsException ex = assertThrows(
                TooManyRequestsException.class,
                () -> service.checkLoginAllowed("ava.sharma@example.com", request)
        );

        assertEquals("Too many login attempts. Please try again later.", ex.getMessage());
        verify(userAccountRepository, never()).findByEmail(anyString());
    }

    @Test
    void recordFailedLoginBlocksAccountOnFifthFailure() {
        UserAccount account = UserAccount.builder()
                .email("ava.sharma@example.com")
                .passwordHash("hash")
                .build();
        when(loginAttemptRepository.countFailedAttemptsByEmailSince(
                eq("ava.sharma@example.com"),
                any(LocalDateTime.class)
        )).thenReturn(5L);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> service.recordFailedLogin("ava.sharma@example.com", request, account)
        );

        assertEquals("Account is blocked. Please reset the password.", ex.getMessage());
        assertTrue(account.isLoginBlocked());
        assertNotNull(account.getLoginBlockedAt());
        verify(userAccountRepository).save(account);
    }

    @Test
    void checkLoginAllowedRejectsBlockedAccount() {
        UserAccount account = UserAccount.builder()
                .email("ava.sharma@example.com")
                .passwordHash("hash")
                .loginBlocked(true)
                .build();
        when(userAccountRepository.findByEmail("ava.sharma@example.com")).thenReturn(Optional.of(account));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> service.checkLoginAllowed("ava.sharma@example.com", request)
        );

        assertEquals("Account is blocked. Please reset the password.", ex.getMessage());
    }

    @Test
    void recordFailedLoginDoesNotRollbackAccountBlockWhenThrowingBlockedMessage() throws Exception {
        Method method = LoginFraudDetectionServiceImpl.class.getMethod(
                "recordFailedLogin",
                String.class,
                HttpServletRequest.class,
                UserAccount.class
        );

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertArrayEquals(new Class<?>[]{BadRequestException.class}, transactional.noRollbackFor());
    }
}
