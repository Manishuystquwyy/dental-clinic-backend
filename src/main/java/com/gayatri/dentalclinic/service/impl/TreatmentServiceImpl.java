package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.dto.request.TreatmentRequestDto;
import com.gayatri.dentalclinic.dto.response.TreatmentResponseDto;
import com.gayatri.dentalclinic.entity.Appointment;
import com.gayatri.dentalclinic.entity.Treatment;
import com.gayatri.dentalclinic.enums.Role;
import com.gayatri.dentalclinic.exception.NotFoundException;
import com.gayatri.dentalclinic.mapper.TreatmentMapper;
import com.gayatri.dentalclinic.repository.AppointmentRepository;
import com.gayatri.dentalclinic.repository.TreatmentRepository;
import com.gayatri.dentalclinic.security.SecurityUtils;
import com.gayatri.dentalclinic.service.TreatmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TreatmentServiceImpl implements TreatmentService {

    private final TreatmentRepository treatmentRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    public TreatmentResponseDto createTreatment(TreatmentRequestDto requestDto) {
        denyIfPatient();
        Appointment appointment = appointmentRepository.findById(requestDto.getAppointmentId())
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + requestDto.getAppointmentId()));

        Treatment treatment = TreatmentMapper.toEntity(requestDto, appointment);
        Treatment savedTreatment = treatmentRepository.save(treatment);
        return TreatmentMapper.toDto(savedTreatment);
    }

    @Override
    public List<TreatmentResponseDto> getAllTreatments() {
        if (SecurityUtils.getCurrentRole() == Role.PATIENT) {
            Long patientId = SecurityUtils.getCurrentPatientId();
            return treatmentRepository.findByAppointmentPatientId(patientId)
                    .stream()
                    .map(TreatmentMapper::toDto)
                    .toList();
        }
        return treatmentRepository.findAll()
                .stream()
                .map(TreatmentMapper::toDto)
                .toList();
    }

    @Override
    public TreatmentResponseDto getTreatmentById(Long id) {
        Treatment treatment = treatmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Treatment not found with id: " + id));
        enforcePatientAccess(treatment.getAppointment().getPatient().getId());
        return TreatmentMapper.toDto(treatment);
    }

    @Override
    public TreatmentResponseDto updateTreatment(Long id, TreatmentRequestDto requestDto) {
        denyIfPatient();
        Treatment treatment = treatmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Treatment not found with id: " + id));

        Appointment appointment = appointmentRepository.findById(requestDto.getAppointmentId())
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + requestDto.getAppointmentId()));

        TreatmentMapper.updateEntity(requestDto, treatment, appointment);
        Treatment savedTreatment = treatmentRepository.save(treatment);
        return TreatmentMapper.toDto(savedTreatment);
    }

    @Override
    public void deleteTreatment(Long id) {
        denyIfPatient();
        Treatment treatment = treatmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Treatment not found with id: " + id));
        treatmentRepository.delete(treatment);
    }

    private void enforcePatientAccess(Long patientId) {
        if (SecurityUtils.getCurrentRole() == Role.PATIENT) {
            Long currentPatientId = SecurityUtils.getCurrentPatientId();
            if (currentPatientId == null || !currentPatientId.equals(patientId)) {
                throw new AccessDeniedException("You can only access your own treatments.");
            }
        }
    }

    private void denyIfPatient() {
        if (SecurityUtils.getCurrentRole() == Role.PATIENT) {
            throw new AccessDeniedException("Patients are not allowed to modify treatments.");
        }
    }
}
