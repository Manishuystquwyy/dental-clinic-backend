package com.gayatri.dentalclinic.controller;

import com.gayatri.dentalclinic.enums.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:appointment-policy;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
        "spring.jpa.open-in-view=false", "app.admin.email=", "app.admin.password=",
        "app.appointment.cancellation-cutoff-hours=24", "app.appointment.reschedule-cutoff-hours=6"
})
@AutoConfigureMockMvc
class AppointmentPolicyControllerTest {
    @Autowired MockMvc mvc;

    @ParameterizedTest
    @EnumSource(Role.class)
    void authenticatedAppointmentRolesReceiveTheConfiguredPolicy(Role role) throws Exception {
        mvc.perform(get("/api/appointments/policy").with(user("user").roles(role.name())))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"cancellationCutoffHours\":24,\"rescheduleCutoffHours\":6}"));
    }

    @Test
    void unauthenticatedRequestsCannotReadThePolicy() throws Exception {
        mvc.perform(get("/api/appointments/policy")).andExpect(status().isForbidden());
    }

    @Test
    void unrelatedRolesCannotReadThePolicy() throws Exception {
        mvc.perform(get("/api/appointments/policy").with(user("other").roles("OTHER")))
                .andExpect(status().isForbidden());
    }
}
