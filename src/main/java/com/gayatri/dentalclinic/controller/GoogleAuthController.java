package com.gayatri.dentalclinic.controller;

import com.gayatri.dentalclinic.dto.request.GoogleLoginRequestDto;
import com.gayatri.dentalclinic.dto.response.GoogleLoginResponseDto;
import com.gayatri.dentalclinic.service.GoogleAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class GoogleAuthController {
    private final GoogleAuthService googleAuthService;
    private final com.gayatri.dentalclinic.service.LoginFraudDetectionService fraudDetection;

    @PostMapping("/google")
    public GoogleLoginResponseDto login(@Valid @RequestBody GoogleLoginRequestDto request, HttpServletRequest servletRequest) {
        // Use the peer address; forwarded headers must be resolved only by a trusted proxy configuration.
        String ip = servletRequest.getRemoteAddr();
        fraudDetection.checkLoginAllowed("", ip);
        try {
            return googleAuthService.login(request, ip);
        } catch (com.gayatri.dentalclinic.exception.BadRequestException ex) {
            // Invalid credentials still count toward the IP rate limit without trusting their email claim.
            fraudDetection.recordFailedLogin("", ip, null);
            throw ex;
        }
    }
}
