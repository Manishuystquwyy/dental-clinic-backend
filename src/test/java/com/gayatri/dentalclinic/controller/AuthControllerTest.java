package com.gayatri.dentalclinic.controller;

import com.gayatri.dentalclinic.dto.request.AuthLoginRequestDto;
import com.gayatri.dentalclinic.dto.response.AuthResponseDto;
import com.gayatri.dentalclinic.service.AuthService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class AuthControllerTest {
    @ParameterizedTest
    @CsvSource(value = {
            "'203.0.113.1, 10.0.0.1', 203.0.113.2, 203.0.113.1",
            "NULL, ' 203.0.113.2 ', 203.0.113.2",
            "'   ', 203.0.113.2, 203.0.113.2",
            "NULL, NULL, 127.0.0.1",
            "'   ', '   ', 127.0.0.1"
    }, nullValues = "NULL")
    void loginExtractsClientIpBeforeCallingService(String forwardedFor, String realIp, String expectedIp) {
        var service = mock(AuthService.class);
        var controller = new AuthController(service);
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        if (forwardedFor != null) request.addHeader("X-Forwarded-For", forwardedFor);
        if (realIp != null) request.addHeader("X-Real-IP", realIp);
        var credentials = new AuthLoginRequestDto();
        var response = mock(AuthResponseDto.class);
        when(service.login(credentials, expectedIp)).thenReturn(response);

        assertSame(response, controller.login(credentials, request));
        verify(service).login(credentials, expectedIp);
        verifyNoMoreInteractions(service);
    }
}
