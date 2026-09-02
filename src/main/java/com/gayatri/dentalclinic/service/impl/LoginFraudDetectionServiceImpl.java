package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.entity.LoginAttempt;
import com.gayatri.dentalclinic.entity.UserAccount;
import com.gayatri.dentalclinic.exception.BadRequestException;
import com.gayatri.dentalclinic.exception.TooManyRequestsException;
import com.gayatri.dentalclinic.repository.LoginAttemptRepository;
import com.gayatri.dentalclinic.repository.UserAccountRepository;
import com.gayatri.dentalclinic.service.LoginFraudDetectionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LoginFraudDetectionServiceImpl implements LoginFraudDetectionService {

    private static final String ACCOUNT_BLOCKED_MESSAGE = "Account is blocked. Please reset the password.";
    private static final String TOO_MANY_ATTEMPTS_MESSAGE = "Too many login attempts. Please try again later.";

    private final LoginAttemptRepository loginAttemptRepository;
    private final UserAccountRepository userAccountRepository;

    @Value("${app.security.login.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.security.login.failed-attempt-window-minutes:15}")
    private long failedAttemptWindowMinutes;

    @Value("${app.security.login.ip-rate-limit-attempts:10}")
    private long ipRateLimitAttempts;

    @Value("${app.security.login.ip-rate-limit-window-minutes:10}")
    private long ipRateLimitWindowMinutes;

    @Override
    @Transactional(readOnly = true)
    public void checkLoginAllowed(String email, HttpServletRequest request) {
        String ipAddress = resolveClientIp(request);
        LocalDateTime ipWindowStart = LocalDateTime.now().minusMinutes(ipRateLimitWindowMinutes);
        long recentIpAttempts = loginAttemptRepository.countByIpAddressAndAttemptedAtGreaterThanEqual(
                ipAddress,
                ipWindowStart
        );
        if (recentIpAttempts >= ipRateLimitAttempts) {
            throw new TooManyRequestsException(TOO_MANY_ATTEMPTS_MESSAGE);
        }

        userAccountRepository.findByEmail(email)
                .filter(UserAccount::isLoginBlocked)
                .ifPresent(account -> {
                    throw new BadRequestException(ACCOUNT_BLOCKED_MESSAGE);
                });
    }

    @Override
    @Transactional
    public void recordSuccessfulLogin(String email, HttpServletRequest request) {
        saveAttempt(email, request, true);
    }

    @Override
    @Transactional(noRollbackFor = BadRequestException.class)
    public void recordFailedLogin(String email, HttpServletRequest request, UserAccount account) {
        saveAttempt(email, request, false);

        if (account == null || account.isLoginBlocked()) {
            return;
        }

        LocalDateTime failedWindowStart = LocalDateTime.now().minusMinutes(failedAttemptWindowMinutes);
        long recentFailures = loginAttemptRepository.countFailedAttemptsByEmailSince(email, failedWindowStart);
        if (recentFailures >= maxFailedAttempts) {
            account.setLoginBlocked(true);
            account.setLoginBlockedAt(LocalDateTime.now());
            userAccountRepository.save(account);
            throw new BadRequestException(ACCOUNT_BLOCKED_MESSAGE);
        }
    }

    private void saveAttempt(String email, HttpServletRequest request, boolean successful) {
        loginAttemptRepository.save(LoginAttempt.builder()
                .email(normalizeEmail(email))
                .ipAddress(resolveClientIp(request))
                .successful(successful)
                .attemptedAt(LocalDateTime.now())
                .build());
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
