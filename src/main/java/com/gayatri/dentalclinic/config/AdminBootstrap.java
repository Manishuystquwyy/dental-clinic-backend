package com.gayatri.dentalclinic.config;

import com.gayatri.dentalclinic.entity.UserAccount;
import com.gayatri.dentalclinic.enums.Role;
import com.gayatri.dentalclinic.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrap implements CommandLineRunner {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (adminEmail == null || adminEmail.isBlank()
                || adminPassword == null || adminPassword.isBlank()) {
            log.info("Admin bootstrap skipped: app.admin.email/password not configured");
            return;
        }
        if (userAccountRepository.existsByEmail(adminEmail)) {
            return;
        }
        UserAccount admin = UserAccount.builder()
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .build();
        userAccountRepository.save(admin);
        log.info("Admin account created for {}", adminEmail);
    }
}
