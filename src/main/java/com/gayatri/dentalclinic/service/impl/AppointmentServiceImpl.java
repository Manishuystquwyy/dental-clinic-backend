package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.dto.request.AppointmentRequestDto;
import com.gayatri.dentalclinic.dto.response.AppointmentResponseDto;
import com.gayatri.dentalclinic.entity.Appointment;
import com.gayatri.dentalclinic.entity.Dentist;
import com.gayatri.dentalclinic.entity.Patient;
import com.gayatri.dentalclinic.enums.AppointmentStatus;
import com.gayatri.dentalclinic.enums.Role;
import com.gayatri.dentalclinic.exception.NotFoundException;
import com.gayatri.dentalclinic.mapper.AppointmentMapper;
import com.gayatri.dentalclinic.repository.AppointmentRepository;
import com.gayatri.dentalclinic.repository.DentistRepository;
import com.gayatri.dentalclinic.repository.PatientRepository;
import com.gayatri.dentalclinic.security.SecurityUtils;
import com.gayatri.dentalclinic.service.AppointmentService;
import com.gayatri.dentalclinic.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DentistRepository dentistRepository;
    private final NotificationService notificationService;

    @Override
    public AppointmentResponseDto createAppointment(AppointmentRequestDto requestDto) {
        enforcePatientAccess(requestDto.getPatientId());
        Patient patient = patientRepository.findById(requestDto.getPatientId())
                .orElseThrow(() -> new NotFoundException("Patient not found with id: " + requestDto.getPatientId()));
        Dentist dentist = dentistRepository.findById(requestDto.getDentistId())
                .orElseThrow(() -> new NotFoundException("Dentist not found with id: " + requestDto.getDentistId()));

        if (requestDto.getStatus() == null) {
            requestDto.setStatus(AppointmentStatus.BOOKED);
        }

        Appointment appointment = AppointmentMapper.toEntity(requestDto, patient, dentist);
        Appointment savedAppointment = appointmentRepository.save(appointment);
        try {
            notificationService.sendAppointmentConfirmation(patient, dentist, savedAppointment);
        } catch (Exception ex) {
            log.warn("Failed to send appointment confirmation notification", ex);
        }
        return AppointmentMapper.toDto(savedAppointment);
    }

    @Override
    public List<AppointmentResponseDto> getAllAppointments() {
        List<Appointment> appointments;
        Long patientId = SecurityUtils.getCurrentPatientId();
        if (SecurityUtils.getCurrentRole() == Role.PATIENT && patientId != null) {
            appointments = appointmentRepository.findByPatientId(patientId);
        } else {
            appointments = appointmentRepository.findAll();
        }
        return appointments
                .stream()
                .map(AppointmentMapper::toDto)
                .toList();
    }

    @Override
    public AppointmentResponseDto getAppointmentById(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + id));
        enforcePatientAccess(appointment.getPatient().getId());
        return AppointmentMapper.toDto(appointment);
    }

    @Override
    public AppointmentResponseDto updateAppointment(Long id, AppointmentRequestDto requestDto) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + id));
        enforcePatientAccess(appointment.getPatient().getId());
        enforcePatientAccess(requestDto.getPatientId());
        denyPatientCancelIfCompleted(appointment, requestDto);

        Patient patient = patientRepository.findById(requestDto.getPatientId())
                .orElseThrow(() -> new NotFoundException("Patient not found with id: " + requestDto.getPatientId()));
        Dentist dentist = dentistRepository.findById(requestDto.getDentistId())
                .orElseThrow(() -> new NotFoundException("Dentist not found with id: " + requestDto.getDentistId()));

        AppointmentMapper.updateEntity(requestDto, appointment, patient, dentist);
        Appointment savedAppointment = appointmentRepository.save(appointment);
        return AppointmentMapper.toDto(savedAppointment);
    }

    @Override
    public void deleteAppointment(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + id));
        enforcePatientAccess(appointment.getPatient().getId());
        if (SecurityUtils.getCurrentRole() == Role.PATIENT
                && appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new AccessDeniedException("Completed appointments cannot be cancelled.");
        }
        appointmentRepository.delete(appointment);
    }

    private void enforcePatientAccess(Long patientId) {
        if (SecurityUtils.getCurrentRole() == Role.PATIENT) {
            Long currentPatientId = SecurityUtils.getCurrentPatientId();
            if (currentPatientId == null || !currentPatientId.equals(patientId)) {
                throw new AccessDeniedException("You can only access your own appointments.");
            }
        }
    }

    private void denyPatientCancelIfCompleted(Appointment appointment, AppointmentRequestDto requestDto) {
        if (SecurityUtils.getCurrentRole() == Role.PATIENT
                && appointment.getStatus() == AppointmentStatus.COMPLETED
                && requestDto.getStatus() == AppointmentStatus.CANCELLED) {
            throw new AccessDeniedException("Completed appointments cannot be cancelled.");
        }
    }
}
