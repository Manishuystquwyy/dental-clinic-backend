package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.dto.request.PatientRequestDto;
import com.gayatri.dentalclinic.dto.response.PatientResponseDto;
import com.gayatri.dentalclinic.entity.Patient;
import com.gayatri.dentalclinic.exception.BadRequestException;
import com.gayatri.dentalclinic.mapper.PatientMapper;
import com.gayatri.dentalclinic.repository.PatientRepository;
import com.gayatri.dentalclinic.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
