package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.entity.LoginAttempt;
import com.gayatri.dentalclinic.entity.UserAccount;
import com.gayatri.dentalclinic.exception.BadRequestException;
import com.gayatri.dentalclinic.exception.TooManyRequestsException;
import com.gayatri.dentalclinic.repository.LoginAttemptRepository;
import com.gayatri.dentalclinic.repository.UserAccountRepository;
import com.gayatri.dentalclinic.service.LoginFraudDetectionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class LoginFraudDetectionServiceImpl implements LoginFraudDetectionService {

    private static final String ACCOUNT_BLOCKED_MESSAGE = "Account is blocked. Please reset the password.";
    private static final String TOO_MANY_ATTEMPTS_MESSAGE = "Too many login attempts. Please try again later.";

    private final LoginAttemptRepository loginAttemptRepository;
    private final UserAccountRepository userAccountRepository;

    private final int maxFailedAttempts;
    private final long failedAttemptWindowMinutes;
    private final long ipRateLimitAttempts;
    private final long ipRateLimitWindowMinutes;

    public LoginFraudDetectionServiceImpl(LoginAttemptRepository loginAttemptRepository,
            UserAccountRepository userAccountRepository,
            @Value("${app.security.login.max-failed-attempts:5}") int maxFailedAttempts,
            @Value("${app.security.login.failed-attempt-window-minutes:15}") long failedAttemptWindowMinutes,
            @Value("${app.security.login.ip-rate-limit-attempts:10}") long ipRateLimitAttempts,
            @Value("${app.security.login.ip-rate-limit-window-minutes:10}") long ipRateLimitWindowMinutes) {
        this.loginAttemptRepository = loginAttemptRepository;
        this.userAccountRepository = userAccountRepository;
        this.maxFailedAttempts = maxFailedAttempts;
        this.failedAttemptWindowMinutes = failedAttemptWindowMinutes;
        this.ipRateLimitAttempts = ipRateLimitAttempts;
        this.ipRateLimitWindowMinutes = ipRateLimitWindowMinutes;
    }

    @Override
    @Transactional(readOnly = true)
    public void checkLoginAllowed(String email, String ipAddress) {
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
    public void recordSuccessfulLogin(String email, String ipAddress) {
        saveAttempt(email, ipAddress, true);
    }

    @Override
    @Transactional(noRollbackFor = BadRequestException.class)
    public void recordFailedLogin(String email, String ipAddress, UserAccount account) {
        saveAttempt(email, ipAddress, false);

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

    private void saveAttempt(String email, String ipAddress, boolean successful) {
        loginAttemptRepository.save(LoginAttempt.builder()
                .email(normalizeEmail(email))
                .ipAddress(ipAddress)
                .successful(successful)
                .attemptedAt(LocalDateTime.now())
                .build());
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
