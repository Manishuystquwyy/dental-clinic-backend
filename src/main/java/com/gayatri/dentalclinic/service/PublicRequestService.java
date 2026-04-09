package com.gayatri.dentalclinic.service;

import com.gayatri.dentalclinic.dto.request.PublicRequestRequestDto;
import com.gayatri.dentalclinic.dto.response.PublicRequestResponseDto;

import java.util.List;

public interface PublicRequestService {

    PublicRequestResponseDto createRequest(PublicRequestRequestDto requestDto);

    List<PublicRequestResponseDto> getAllRequests();
}
