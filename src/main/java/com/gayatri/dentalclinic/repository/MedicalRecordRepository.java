package com.gayatri.dentalclinic.repository;

import com.gayatri.dentalclinic.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    List<MedicalRecord> findByPatientIdOrderByCreatedAtDescIdDesc(Long patientId);
    List<MedicalRecord> findByAppointmentIdAndDentistIdOrderByCreatedAtDescIdDesc(Long appointmentId, Long dentistId);
    boolean existsByAppointmentId(Long appointmentId);
}
