package com.gayatri.dentalclinic.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_attempts", indexes = {
        @Index(name = "idx_login_attempt_email_time", columnList = "email, attemptedAt"),
        @Index(name = "idx_login_attempt_ip_time", columnList = "ipAddress, attemptedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 64)
    private String ipAddress;

    @Column(nullable = false)
    private boolean successful;

    @Column(nullable = false)
    private LocalDateTime attemptedAt;
}
