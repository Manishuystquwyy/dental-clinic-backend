package com.gayatri.dentalclinic.repository;

import com.gayatri.dentalclinic.entity.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TreatmentRepository extends JpaRepository<Treatment, Long> {
}
