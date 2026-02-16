package com.gayatri.dentalclinic.repository;

import com.gayatri.dentalclinic.entity.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TreatmentRepository extends JpaRepository<Treatment, Long> {
    List<Treatment> findByAppointmentPatientId(Long patientId);
}
