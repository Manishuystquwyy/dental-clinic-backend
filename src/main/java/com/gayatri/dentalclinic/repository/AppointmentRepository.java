package com.gayatri.dentalclinic.repository;

import com.gayatri.dentalclinic.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;
import com.gayatri.dentalclinic.enums.AppointmentStatus;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    @Override
    @EntityGraph(attributePaths = "patient")
    List<Appointment> findAll();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.id = :id")
    Optional<Appointment> findWithLockById(Long id);

    @EntityGraph(attributePaths = "patient")
    List<Appointment> findByPatientId(Long patientId);

    @EntityGraph(attributePaths = "patient")
    List<Appointment> findByDentistIdOrderByAppointmentDateAscAppointmentTimeAsc(Long dentistId);

    List<Appointment> findByDentistIdAndAppointmentDateAndStatusIn(
            Long dentistId, LocalDate appointmentDate, List<AppointmentStatus> statuses);

    boolean existsByDentistIdAndAppointmentDateAndAppointmentTimeAndStatusIn(
            Long dentistId, LocalDate appointmentDate, LocalTime appointmentTime, List<AppointmentStatus> statuses);
}
