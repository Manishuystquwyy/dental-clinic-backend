package com.gayatri.dentalclinic.controller;

import com.gayatri.dentalclinic.dto.request.AuthLoginRequestDto;
import com.gayatri.dentalclinic.dto.request.AuthRegisterRequestDto;
import com.gayatri.dentalclinic.dto.response.AuthResponseDto;
import com.gayatri.dentalclinic.dto.response.UserInfoDto;
import com.gayatri.dentalclinic.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a patient account")
    @ApiResponse(responseCode = "201", description = "Account created")
    public AuthResponseDto register(@Valid @RequestBody AuthRegisterRequestDto requestDto) {
        return authService.registerPatient(requestDto);
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    @ApiResponse(responseCode = "200", description = "Logged in")
    public AuthResponseDto login(@Valid @RequestBody AuthLoginRequestDto requestDto) {
        return authService.login(requestDto);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user info")
    @ApiResponse(responseCode = "200", description = "Current user")
    public UserInfoDto me() {
        return authService.getCurrentUser();
    }
}
