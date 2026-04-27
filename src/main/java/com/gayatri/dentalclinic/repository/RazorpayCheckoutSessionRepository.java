package com.gayatri.dentalclinic.repository;

import com.gayatri.dentalclinic.entity.RazorpayCheckoutSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RazorpayCheckoutSessionRepository extends JpaRepository<RazorpayCheckoutSession, Long> {

    Optional<RazorpayCheckoutSession> findByRazorpayOrderId(String razorpayOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from RazorpayCheckoutSession s where s.razorpayOrderId = :razorpayOrderId")
    Optional<RazorpayCheckoutSession> findWithLockByRazorpayOrderId(@Param("razorpayOrderId") String razorpayOrderId);
}
