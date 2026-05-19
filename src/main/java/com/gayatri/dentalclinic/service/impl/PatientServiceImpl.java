package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.config.CacheNames;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;

    @Override
    @CacheEvict(cacheNames = CacheNames.PATIENTS, allEntries = true)
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
    @Cacheable(cacheNames = CacheNames.PATIENTS, sync = true)
    public List<PatientResponseDto> getAllPatients() {
        return patientRepository.findAll()
                .stream()
                .map(PatientMapper::toDto)
                .toList();
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PATIENT_BY_ID, key = "#id", sync = true)
    public PatientResponseDto getPatientById(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Patient not found with id: " + id));
        return PatientMapper.toDto(patient);
    }

    @Override
    @Caching(
            put = @CachePut(cacheNames = CacheNames.PATIENT_BY_ID, key = "#id"),
            evict = @CacheEvict(cacheNames = CacheNames.PATIENTS, allEntries = true)
    )
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
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PATIENT_BY_ID, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.PATIENTS, allEntries = true)
    })
    public void deletePatient(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Patient not found with id: " + id));
        patientRepository.delete(patient);
    }

    @Override
    @Cacheable(
            cacheNames = CacheNames.PATIENT_BY_ID,
            key = "T(com.gayatri.dentalclinic.security.SecurityUtils).getCurrentPatientId()",
            condition = "T(com.gayatri.dentalclinic.security.SecurityUtils).getCurrentPatientId() != null",
            sync = true
    )
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
    @Caching(
            put = @CachePut(
                    cacheNames = CacheNames.PATIENT_BY_ID,
                    key = "T(com.gayatri.dentalclinic.security.SecurityUtils).getCurrentPatientId()"
            ),
            evict = @CacheEvict(cacheNames = CacheNames.PATIENTS, allEntries = true)
    )
    public PatientResponseDto updateCurrentPatient(PatientRequestDto requestDto) {
        Long patientId = SecurityUtils.getCurrentPatientId();
        if (patientId == null) {
            throw new AccessDeniedException("No patient account found.");
        }

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new NotFoundException("Patient not found with id: " + patientId));

        if (patientRepository.existsByPhoneAndIdNot(requestDto.getPhone(), patientId)) {
            throw new BadRequestException("Phone number already exists");
        }
        if (requestDto.getEmail() != null &&
                patientRepository.existsByEmailAndIdNot(requestDto.getEmail(), patientId)) {
            throw new BadRequestException("Email already exists");
        }

        PatientMapper.updateEntity(requestDto, patient);
        Patient savedPatient = patientRepository.save(patient);
        return PatientMapper.toDto(savedPatient);
    }
}
