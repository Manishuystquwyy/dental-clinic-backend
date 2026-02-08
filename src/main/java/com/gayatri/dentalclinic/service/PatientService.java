package com.gayatri.dentalclinic.service;

import com.gayatri.dentalclinic.dto.request.PatientRequestDto;
import com.gayatri.dentalclinic.dto.response.PatientResponseDto;

import java.util.List;

public interface PatientService {

    PatientResponseDto createPatient(PatientRequestDto requestDto);
    List<PatientResponseDto> getAllPatients();
    PatientResponseDto getPatientById(Long id);
    PatientResponseDto updatePatient(Long id, PatientRequestDto requestDto);
    void deletePatient(Long id);
    PatientResponseDto getCurrentPatient();
    PatientResponseDto updateCurrentPatient(PatientRequestDto requestDto);

}
