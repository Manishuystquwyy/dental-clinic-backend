package com.gayatri.dentalclinic.controller;

import com.gayatri.dentalclinic.dto.request.PublicRequestRequestDto;
import com.gayatri.dentalclinic.dto.response.PublicRequestResponseDto;
import com.gayatri.dentalclinic.service.PublicRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public-requests")
@RequiredArgsConstructor
@Tag(name = "Public Requests", description = "Public contact and booking request endpoints")
public class PublicRequestController {

    private final PublicRequestService publicRequestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a public request")
    @ApiResponse(responseCode = "201", description = "Request created")
    public PublicRequestResponseDto createRequest(@Valid @RequestBody PublicRequestRequestDto requestDto) {
        return publicRequestService.createRequest(requestDto);
    }

    @GetMapping
    @Operation(summary = "List public requests")
    @ApiResponse(responseCode = "200", description = "Request list")
    public List<PublicRequestResponseDto> getAllRequests() {
        return publicRequestService.getAllRequests();
    }
}
