package com.gayatri.dentalclinic.repository;

import com.gayatri.dentalclinic.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill> findByAppointmentPatientId(Long patientId);
}
