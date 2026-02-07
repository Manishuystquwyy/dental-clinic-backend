package com.gayatri.dentalclinic.service;

import com.gayatri.dentalclinic.dto.request.PatientRequestDto;
import com.gayatri.dentalclinic.dto.response.PatientResponseDto;

public interface PatientService {

    PatientResponseDto createPatient(PatientRequestDto requestDto);

}
