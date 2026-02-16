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
        dentist.setQualification(dto.getQualification());
        dentist.setSpecialization(dto.getSpecialization());
        dentist.setPictureUrl(dto.getPictureUrl());
    }

    public static DentistResponseDto toDto(Dentist dentist) {
        return DentistResponseDto.builder()
                .id(dentist.getId())
                .name(dentist.getName())
                .phone(dentist.getPhone())
                .email(dentist.getEmail())
                .experienceYears(dentist.getExperienceYears())
                .qualification(dentist.getQualification())
                .specialization(dentist.getSpecialization())
                .pictureUrl(dentist.getPictureUrl())
                .build();
    }
}
