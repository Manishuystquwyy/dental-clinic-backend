package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.dto.request.PatientRequestDto;
import com.gayatri.dentalclinic.dto.response.PatientResponseDto;
import com.gayatri.dentalclinic.entity.Patient;
import com.gayatri.dentalclinic.exception.BadRequestException;
import com.gayatri.dentalclinic.exception.NotFoundException;
import com.gayatri.dentalclinic.mapper.PatientMapper;
import com.gayatri.dentalclinic.repository.PatientRepository;
import com.gayatri.dentalclinic.security.SecurityUtils;
import com.gayatri.dentalclinic.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;

    @Override
    public PatientResponseDto createPatient(PatientRequestDto requestDto) {
        if(patientRepository.existsByPhone(requestDto.getPhone())){
            throw new BadRequestException("Phone number already exists");
        }
        if (requestDto.getEmail() != null &&
                patientRepository.existsByEmail(requestDto.getEmail())) {
            throw new BadRequestException("Email already exists");
        }

        Patient patient = PatientMapper.toEntity(requestDto);
        Patient savedPatient = patientRepository.save(patient);

        return PatientMapper.toDto(savedPatient);
    }

    @Override
    public List<PatientResponseDto> getAllPatients() {
        return patientRepository.findAll()
                .stream()
                .map(PatientMapper::toDto)
                .toList();
    }

    @Override
    public PatientResponseDto getPatientById(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Patient not found with id: " + id));
        return PatientMapper.toDto(patient);
    }

    @Override
    public PatientResponseDto updatePatient(Long id, PatientRequestDto requestDto) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Patient not found with id: " + id));

        if (patientRepository.existsByPhoneAndIdNot(requestDto.getPhone(), id)) {
            throw new BadRequestException("Phone number already exists");
        }
        if (requestDto.getEmail() != null &&
                patientRepository.existsByEmailAndIdNot(requestDto.getEmail(), id)) {
            throw new BadRequestException("Email already exists");
        }

        PatientMapper.updateEntity(requestDto, patient);
        Patient savedPatient = patientRepository.save(patient);
        return PatientMapper.toDto(savedPatient);
    }

    @Override
    public void deletePatient(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Patient not found with id: " + id));
        patientRepository.delete(patient);
    }

    @Override
    public PatientResponseDto getCurrentPatient() {
        Long patientId = SecurityUtils.getCurrentPatientId();
        if (patientId == null) {
            throw new AccessDeniedException("No patient account found.");
        }
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new NotFoundException("Patient not found with id: " + patientId));
        return PatientMapper.toDto(patient);
    }

    @Override
    public PatientResponseDto updateCurrentPatient(PatientRequestDto requestDto) {
        Long patientId = SecurityUtils.getCurrentPatientId();
        if (patientId == null) {
            throw new AccessDeniedException("No patient account found.");
        }
        return updatePatient(patientId, requestDto);
    }
}
