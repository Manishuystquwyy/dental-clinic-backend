package com.gayatri.dentalclinic.service;

import com.gayatri.dentalclinic.dto.request.TreatmentRequestDto;
import com.gayatri.dentalclinic.dto.response.TreatmentResponseDto;

import java.util.List;

public interface TreatmentService {

    TreatmentResponseDto createTreatment(TreatmentRequestDto requestDto);
    List<TreatmentResponseDto> getAllTreatments();
    TreatmentResponseDto getTreatmentById(Long id);
    TreatmentResponseDto updateTreatment(Long id, TreatmentRequestDto requestDto);
    void deleteTreatment(Long id);
}
