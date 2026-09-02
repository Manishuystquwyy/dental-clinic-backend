package com.gayatri.dentalclinic.config;

import com.gayatri.dentalclinic.entity.UserAccount;
import com.gayatri.dentalclinic.enums.Role;
import com.gayatri.dentalclinic.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrap(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

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

        userAccountRepository.findByEmail(adminEmail).ifPresentOrElse(existingAdmin -> {
            boolean changed = false;
            if (!passwordEncoder.matches(adminPassword, existingAdmin.getPasswordHash())) {
                existingAdmin.setPasswordHash(passwordEncoder.encode(adminPassword));
                changed = true;
            }
            if (existingAdmin.getRole() != Role.ADMIN) {
                existingAdmin.setRole(Role.ADMIN);
                changed = true;
            }
            if (changed) {
                userAccountRepository.save(existingAdmin);
                log.info("Admin account updated for {}", adminEmail);
            }
        }, () -> {
            UserAccount admin = new UserAccount();
            admin.setEmail(adminEmail);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            userAccountRepository.save(admin);
            log.info("Admin account created for {}", adminEmail);
        });
    }
}
