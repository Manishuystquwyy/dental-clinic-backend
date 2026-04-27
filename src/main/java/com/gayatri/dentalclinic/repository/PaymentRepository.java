package com.gayatri.dentalclinic.repository;

import com.gayatri.dentalclinic.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBillAppointmentPatientId(Long patientId);
    boolean existsByGatewayPaymentId(String gatewayPaymentId);
    Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId);
}
