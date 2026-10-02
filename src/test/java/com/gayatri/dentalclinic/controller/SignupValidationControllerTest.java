package com.gayatri.dentalclinic.controller;

import com.gayatri.dentalclinic.dto.request.AuthRegisterRequestDto;
import com.gayatri.dentalclinic.exception.GlobalExceptionHandler;
import com.gayatri.dentalclinic.exception.SignupValidationException;
import com.gayatri.dentalclinic.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SignupValidationControllerTest {
    AuthService service;
    MockMvc mvc;
    @BeforeEach void setUp() {
        service = mock(AuthService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    String payload(String phone, String password) {
        return """
                {"firstName":" Ava ","lastName":" Sharma ","email":" Ava@EXAMPLE.COM ",
                 "phone":"%s","password":"%s","address":" ","gender":" Female "}
                """.formatted(phone, password);
    }
    @Test void normalizesJsonBeforeServiceCall() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(payload("9876543210", "password123"))).andExpect(status().isCreated());
        var argument = ArgumentCaptor.forClass(AuthRegisterRequestDto.class);
        verify(service).registerPatient(argument.capture());
        assertEquals("ava@example.com", argument.getValue().getEmail());
        assertEquals("Ava", argument.getValue().getFirstName());
        assertNull(argument.getValue().getAddress()); assertEquals("Female", argument.getValue().getGender());
    }
    @ParameterizedTest
    @ValueSource(strings = {"abcdefghij", "1231231223", "5234567890", "987654321", "98765432101"})
    void invalidPhoneNeverReachesService(String phone) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(payload(phone, "password123")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.phone").isString());
        verifyNoInteractions(service);
    }
    @Test void oversizedPasswordErrorUsesPasswordField() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(payload("9876543210", "é".repeat(37))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.password").isString());
        verifyNoInteractions(service);
    }
    @Test void duplicateEmailReturnsFieldError() throws Exception {
        when(service.registerPatient(any())).thenThrow(new SignupValidationException("email", "Email already exists"));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(payload("9876543210", "password123")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.email").value("Email already exists"));
    }
    @ParameterizedTest
    @ValueSource(strings = {"mkhh@gmail", "user@localhost", "user@gmail.", "user@.com", "user@gmail..com", "user@@gmail.com"})
    void invalidEmailNeverReachesService(String email) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(payload("9876543210", "password123").replace(" Ava@EXAMPLE.COM ", email)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.email").value("Invalid email format"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"mkhh@gmail.com", "user+clinic@example.co.in", " User@Mail.Example.COM "})
    void acceptsEmailWithDomainSuffix(String email) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(payload("9876543210", "password123").replace(" Ava@EXAMPLE.COM ", email)))
                .andExpect(status().isCreated());
        verify(service).registerPatient(any());
    }
    @ParameterizedTest
    @ValueSource(strings = {"3123", "Ava123", "१२३", "Ava٣", "@@@", "---"})
    void invalidNamesNeverReachService(String name) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(payload("9876543210", "password123").replace(" Ava ", name).replace(" Sharma ", name)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.firstName").isString()).andExpect(jsonPath("$.lastName").isString());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"राज", "O'Neil-Smith", "D’Arcy", "Mary Jane", "Jose\u0301", "李", " Ava "})
    void acceptsInternationalNames(String name) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(payload("9876543210", "password123").replace(" Ava ", name).replace(" Sharma ", name)))
                .andExpect(status().isCreated());
        verify(service).registerPatient(any());
    }
    @Test void missingGenderNeverReachesService() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(payload("9876543210", "password123").replace(" Female ", " ")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.gender").value("Gender is required"));
        verifyNoInteractions(service);
    }

    @Test void ancientBirthDateReturnsDateFieldError() throws Exception {
        String body = payload("9876543210", "password123").replace("}", ", \"dateOfBirth\": \"0001-10-12\"}");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.dateOfBirth").value("Age cannot exceed 100 years"));
        verifyNoInteractions(service);
    }
    @ParameterizedTest
    @ValueSource(strings = {"232131", " 232131 ", "१२३४", "23/14, 411001", "---"})
    void addressWithoutLettersNeverReachesService(String address) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(payload("9876543210", "password123").replace("\"address\":\" \"", "\"address\":\"" + address + "\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.address").value("Address must include a street, area, or place name"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "23 MG Road, Pune 411001", "१२, पुणे", "Apt #2-B, Main St."})
    void acceptsOptionalAddressAndMixedText(String address) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(payload("9876543210", "password123").replace("\"address\":\" \"", "\"address\":\"" + address + "\"")))
                .andExpect(status().isCreated());
        verify(service).registerPatient(any());
    }
}
