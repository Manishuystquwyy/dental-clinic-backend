package com.gayatri.dentalclinic.repository;

import com.gayatri.dentalclinic.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
}
