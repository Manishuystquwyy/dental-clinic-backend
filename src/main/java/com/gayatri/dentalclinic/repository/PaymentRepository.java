package com.gayatri.dentalclinic.repository;

import com.gayatri.dentalclinic.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
