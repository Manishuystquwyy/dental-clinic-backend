package com.gayatri.dentalclinic.repository;

import com.gayatri.dentalclinic.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    // Login builds patient details after this repository transaction has completed.
    @EntityGraph(attributePaths = "patient")
    Optional<UserAccount> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<UserAccount> findByResetTokenHash(String resetTokenHash);
}
