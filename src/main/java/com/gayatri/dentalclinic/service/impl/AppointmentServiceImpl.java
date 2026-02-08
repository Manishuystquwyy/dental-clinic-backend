package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.dto.request.AppointmentRequestDto;
import com.gayatri.dentalclinic.dto.response.AppointmentResponseDto;
import com.gayatri.dentalclinic.entity.Appointment;
import com.gayatri.dentalclinic.entity.Dentist;
import com.gayatri.dentalclinic.entity.Patient;
import com.gayatri.dentalclinic.enums.AppointmentStatus;
import com.gayatri.dentalclinic.exception.NotFoundException;
import com.gayatri.dentalclinic.mapper.AppointmentMapper;
import com.gayatri.dentalclinic.repository.AppointmentRepository;
import com.gayatri.dentalclinic.repository.DentistRepository;
import com.gayatri.dentalclinic.repository.PatientRepository;
import com.gayatri.dentalclinic.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DentistRepository dentistRepository;

    @Override
    public AppointmentResponseDto createAppointment(AppointmentRequestDto requestDto) {
        Patient patient = patientRepository.findById(requestDto.getPatientId())
                .orElseThrow(() -> new NotFoundException("Patient not found with id: " + requestDto.getPatientId()));
        Dentist dentist = dentistRepository.findById(requestDto.getDentistId())
                .orElseThrow(() -> new NotFoundException("Dentist not found with id: " + requestDto.getDentistId()));

        if (requestDto.getStatus() == null) {
            requestDto.setStatus(AppointmentStatus.BOOKED);
        }

        Appointment appointment = AppointmentMapper.toEntity(requestDto, patient, dentist);
        Appointment savedAppointment = appointmentRepository.save(appointment);
        return AppointmentMapper.toDto(savedAppointment);
    }

    @Override
    public List<AppointmentResponseDto> getAllAppointments() {
        return appointmentRepository.findAll()
                .stream()
                .map(AppointmentMapper::toDto)
                .toList();
    }

    @Override
    public AppointmentResponseDto getAppointmentById(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + id));
        return AppointmentMapper.toDto(appointment);
    }

    @Override
    public AppointmentResponseDto updateAppointment(Long id, AppointmentRequestDto requestDto) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + id));

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
        appointmentRepository.delete(appointment);
    }
}
