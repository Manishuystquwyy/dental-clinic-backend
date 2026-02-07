package com.gayatri.dentalclinic.mapper;

import com.gayatri.dentalclinic.dto.request.PatientRequestDto;
import com.gayatri.dentalclinic.dto.response.PatientResponseDto;
import com.gayatri.dentalclinic.entity.Patient;

import java.time.LocalDateTime;

public class PatientMapper {

    public static Patient toEntity(PatientRequestDto dto) {
        return Patient.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .gender(dto.getGender())
                .dateOfBirth(dto.getDateOfBirth())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .address(dto.getAddress())
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static void updateEntity(PatientRequestDto dto, Patient patient) {
        patient.setFirstName(dto.getFirstName());
        patient.setLastName(dto.getLastName());
        patient.setGender(dto.getGender());
        patient.setDateOfBirth(dto.getDateOfBirth());
        patient.setPhone(dto.getPhone());
        patient.setEmail(dto.getEmail());
        patient.setAddress(dto.getAddress());
    }

    public static PatientResponseDto toDto(Patient patient) {
        return PatientResponseDto.builder()
                .id(patient.getId())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .gender(patient.getGender())
                .dateOfBirth(patient.getDateOfBirth())
                .phone(patient.getPhone())
                .email(patient.getEmail())
                .address(patient.getAddress())
                .createdAt(patient.getCreatedAt())
                .build();
    }
}
