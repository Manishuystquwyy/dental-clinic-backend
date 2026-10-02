package com.gayatri.dentalclinic.controller;

import com.gayatri.dentalclinic.entity.UserAccount;
import com.gayatri.dentalclinic.enums.Role;
import com.gayatri.dentalclinic.repository.UserAccountRepository;
import com.gayatri.dentalclinic.repository.DentistRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:doctor-registration-security;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.admin.email=",
        "app.admin.password="
})
@AutoConfigureMockMvc
class AdminDoctorControllerSecurityTest {

    private static final String REQUEST_BODY = """
            {
              "fullName": "Dr. Riya Kapoor",
              "email": "riya.kapoor@example.com",
              "phone": "9876543210",
              "password": "StrongPass@123",
              "specialization": "Orthodontist",
              "qualification": "BDS",
              "experienceYears": 8,
              "consultationFee": 500.00
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DentistRepository dentistRepository;

    @Test
    void registerDoctorRejectsNonAdminUsers() throws Exception {
        mockMvc.perform(post("/api/admin/doctors/register")
                        .with(user("patient").roles("PATIENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void registerDoctorAllowsAdmins() throws Exception {
        mockMvc.perform(post("/api/admin/doctors/register")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("DOCTOR"))
                .andExpect(jsonPath("$.password").doesNotExist());

        UserAccount account = userAccountRepository.findByEmail("riya.kapoor@example.com").orElseThrow();
        assertEquals(Role.DOCTOR, account.getRole());
        assertNotNull(account.getDentist());
        assertEquals("Orthodontist", dentistRepository.findById(account.getDentist().getId()).orElseThrow().getSpecialization());
        assertNotEquals("StrongPass@123", account.getPasswordHash());
        assertTrue(passwordEncoder.matches("StrongPass@123", account.getPasswordHash()));
    }
}
