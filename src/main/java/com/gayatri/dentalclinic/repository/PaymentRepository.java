package com.gayatri.dentalclinic.repository;

import com.gayatri.dentalclinic.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Collection;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByBillAppointmentPatientId(Long patientId);
    boolean existsByGatewayPaymentId(String gatewayPaymentId);
    Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId);
    boolean existsByBillId(Long billId);
    List<Payment> findByBillAppointmentId(Long appointmentId);
    @EntityGraph(attributePaths = {"bill", "bill.appointment"})
    List<Payment> findByBillAppointmentIdIn(Collection<Long> appointmentIds);
}
