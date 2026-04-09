package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.dto.request.PublicRequestRequestDto;
import com.gayatri.dentalclinic.dto.response.PublicRequestResponseDto;
import com.gayatri.dentalclinic.entity.PublicRequest;
import com.gayatri.dentalclinic.repository.PublicRequestRepository;
import com.gayatri.dentalclinic.service.NotificationService;
import com.gayatri.dentalclinic.service.PublicRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicRequestServiceImpl implements PublicRequestService {

    private final PublicRequestRepository publicRequestRepository;
    private final NotificationService notificationService;

    @Override
    public PublicRequestResponseDto createRequest(PublicRequestRequestDto requestDto) {
        PublicRequest request = PublicRequest.builder()
                .name(requestDto.getName().trim())
                .phone(requestDto.getPhone().trim())
                .message(requestDto.getMessage().trim())
                .requestType(requestDto.getRequestType())
                .createdAt(LocalDateTime.now())
                .build();

        PublicRequest savedRequest = publicRequestRepository.save(request);
        try {
            notificationService.sendPublicRequestNotification(savedRequest);
        } catch (Exception ex) {
            log.warn("Failed to send public request notification for request {}", savedRequest.getId(), ex);
        }

        return toDto(savedRequest);
    }

    @Override
    public List<PublicRequestResponseDto> getAllRequests() {
        return publicRequestRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    private PublicRequestResponseDto toDto(PublicRequest request) {
        return PublicRequestResponseDto.builder()
                .id(request.getId())
                .name(request.getName())
                .phone(request.getPhone())
                .message(request.getMessage())
                .requestType(request.getRequestType())
                .createdAt(request.getCreatedAt())
                .build();
    }
}
