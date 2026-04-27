package com.gayatri.dentalclinic.mapper;

import com.gayatri.dentalclinic.dto.request.DentistRequestDto;
import com.gayatri.dentalclinic.dto.response.DentistResponseDto;
import com.gayatri.dentalclinic.entity.Dentist;

public class DentistMapper {

    public static Dentist toEntity(DentistRequestDto dto) {
        return Dentist.builder()
                .name(dto.getName())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .experienceYears(dto.getExperienceYears())
                .consultationFees(dto.getConsultationFees())
                .qualification(dto.getQualification())
                .specialization(dto.getSpecialization())
                .pictureUrl(dto.getPictureUrl())
                .build();
    }

    public static void updateEntity(DentistRequestDto dto, Dentist dentist) {
        dentist.setName(dto.getName());
        dentist.setPhone(dto.getPhone());
        dentist.setEmail(dto.getEmail());
        dentist.setExperienceYears(dto.getExperienceYears());
        dentist.setConsultationFees(dto.getConsultationFees());
        dentist.setQualification(dto.getQualification());
        dentist.setSpecialization(dto.getSpecialization());
        dentist.setPictureUrl(dto.getPictureUrl());
    }

    public static DentistResponseDto toDto(Dentist dentist) {
        DentistResponseDto dto = new DentistResponseDto();
        dto.setId(dentist.getId());
        dto.setName(dentist.getName());
        dto.setPhone(dentist.getPhone());
        dto.setEmail(dentist.getEmail());
        dto.setExperienceYears(dentist.getExperienceYears());
        dto.setConsultationFees(dentist.getConsultationFees());
        dto.setQualification(dentist.getQualification());
        dto.setSpecialization(dentist.getSpecialization());
        dto.setPictureUrl(dentist.getPictureUrl());
        return dto;
    }
}
