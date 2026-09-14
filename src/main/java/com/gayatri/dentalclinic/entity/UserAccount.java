package com.gayatri.dentalclinic.entity;

import com.gayatri.dentalclinic.enums.Role;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @OneToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;

    private String resetTokenHash;

    private java.time.LocalDateTime resetTokenExpiry;

    @Column(nullable = false)
    @Builder.Default
    private boolean loginBlocked = false;

    private java.time.LocalDateTime loginBlockedAt;

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
