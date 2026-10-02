package com.gayatri.dentalclinic.controller;

import com.gayatri.dentalclinic.exception.BadRequestException;
import com.gayatri.dentalclinic.exception.GlobalExceptionHandler;
import com.gayatri.dentalclinic.exception.TooManyRequestsException;
import com.gayatri.dentalclinic.service.GoogleAuthService;
import com.gayatri.dentalclinic.service.LoginFraudDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GoogleAuthControllerTest {
    private final GoogleAuthService service = mock(GoogleAuthService.class);
    private final LoginFraudDetectionService fraud = mock(LoginFraudDetectionService.class);
    private MockMvc mvc;

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new GoogleAuthController(service, fraud))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test void emptyCredentialRejectedBeforeVerification() throws Exception {
        mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON).content("{\"credential\":\"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service, fraud);
    }

    @Test void invalidPatientProfileRejectedBeforeCreation() throws Exception {
        mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON).content("""
                {"credential":"token","profile":{"firstName":"Ava","lastName":"Sharma","phone":"123","gender":"Female"}}
                """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$['profile.phone']").exists());
        verifyNoInteractions(service);
    }

    @Test void failedVerificationCountsTowardIpLimit() throws Exception {
        when(service.login(any(), anyString())).thenThrow(new BadRequestException("Invalid Google token"));
        mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON).content("{\"credential\":\"token\"}"))
                .andExpect(status().isBadRequest());
        verify(fraud).recordFailedLogin("", "127.0.0.1", null);
    }

    @Test void ipLimitStopsExpensiveTokenVerification() throws Exception {
        doThrow(new TooManyRequestsException("Too many attempts")).when(fraud).checkLoginAllowed("", "127.0.0.1");
        mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON).content("{\"credential\":\"token\"}"))
                .andExpect(status().isTooManyRequests());
        verifyNoInteractions(service);
    }
}
