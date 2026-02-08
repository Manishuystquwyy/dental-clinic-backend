package com.gayatri.dentalclinic.service;

import com.gayatri.dentalclinic.dto.request.DentistRequestDto;
import com.gayatri.dentalclinic.dto.response.DentistResponseDto;

import java.util.List;

public interface DentistService {

    DentistResponseDto createDentist(DentistRequestDto requestDto);
    List<DentistResponseDto> getAllDentists();
    DentistResponseDto getDentistById(Long id);
    DentistResponseDto updateDentist(Long id, DentistRequestDto requestDto);
    void deleteDentist(Long id);
}
