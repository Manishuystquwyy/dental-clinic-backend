package com.gayatri.dentalclinic.repository;

import com.gayatri.dentalclinic.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;
import com.gayatri.dentalclinic.enums.AppointmentStatus;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientId(Long patientId);

    List<Appointment> findByDentistIdAndAppointmentDateAndStatusIn(
            Long dentistId, LocalDate appointmentDate, List<AppointmentStatus> statuses);

    boolean existsByDentistIdAndAppointmentDateAndAppointmentTimeAndStatusIn(
            Long dentistId, LocalDate appointmentDate, LocalTime appointmentTime, List<AppointmentStatus> statuses);
}
