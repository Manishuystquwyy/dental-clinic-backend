package com.gayatri.dentalclinic.repository;

import com.gayatri.dentalclinic.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
}
