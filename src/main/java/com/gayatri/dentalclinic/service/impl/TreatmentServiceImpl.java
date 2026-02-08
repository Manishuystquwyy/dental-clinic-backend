package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.dto.request.TreatmentRequestDto;
import com.gayatri.dentalclinic.dto.response.TreatmentResponseDto;
import com.gayatri.dentalclinic.entity.Appointment;
import com.gayatri.dentalclinic.entity.Treatment;
import com.gayatri.dentalclinic.exception.NotFoundException;
import com.gayatri.dentalclinic.mapper.TreatmentMapper;
import com.gayatri.dentalclinic.repository.AppointmentRepository;
import com.gayatri.dentalclinic.repository.TreatmentRepository;
import com.gayatri.dentalclinic.service.TreatmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TreatmentServiceImpl implements TreatmentService {

    private final TreatmentRepository treatmentRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    public TreatmentResponseDto createTreatment(TreatmentRequestDto requestDto) {
        Appointment appointment = appointmentRepository.findById(requestDto.getAppointmentId())
                .orElseThrow(() -> new NotFoundException("Appointment not found with id: " + requestDto.getAppointmentId()));

        Treatment treatment = TreatmentMapper.toEntity(requestDto, appointment);
        Treatment savedTreatment = treatmentRepository.save(treatment);
        return TreatmentMapper.toDto(savedTreatment);
    }

    @Override
    public List<TreatmentResponseDto> getAllTreatments() {
        return treatmentRepository.findAll()
                .stream()
                .map(TreatmentMapper::toDto)
                .toList();
    }

    @Override
    public TreatmentResponseDto getTreatmentById(Long id) {
        Treatment treatment = treatmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Treatment not found with id: " + id));
        return TreatmentMapper.toDto(treatment);
    }

    @Override
    public TreatmentResponseDto updateTreatment(Long id, TreatmentRequestDto requestDto) {
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
        Treatment treatment = treatmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Treatment not found with id: " + id));
        treatmentRepository.delete(treatment);
    }
}
