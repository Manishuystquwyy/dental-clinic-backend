package com.gayatri.dentalclinic.repository;

import com.gayatri.dentalclinic.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    @Query("""
            select count(a)
            from LoginAttempt a
            where lower(a.email) = lower(:email)
              and a.successful = false
              and a.attemptedAt >= :since
            """)
    long countFailedAttemptsByEmailSince(@Param("email") String email, @Param("since") LocalDateTime since);

    long countByIpAddressAndAttemptedAtGreaterThanEqual(String ipAddress, LocalDateTime since);
}
