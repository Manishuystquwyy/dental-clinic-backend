package com.gayatri.dentalclinic.repository;

import com.gayatri.dentalclinic.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBillAppointmentPatientId(Long patientId);
}
