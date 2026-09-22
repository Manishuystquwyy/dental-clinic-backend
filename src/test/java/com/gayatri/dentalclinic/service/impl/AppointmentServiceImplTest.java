package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.dto.response.AppointmentResponseDto;
import com.gayatri.dentalclinic.entity.Appointment;
import com.gayatri.dentalclinic.entity.Dentist;
import com.gayatri.dentalclinic.entity.Patient;
import com.gayatri.dentalclinic.entity.UserAccount;
import com.gayatri.dentalclinic.enums.AppointmentStatus;
import com.gayatri.dentalclinic.enums.Role;
import com.gayatri.dentalclinic.repository.AppointmentRepository;
import com.gayatri.dentalclinic.repository.DentistRepository;
import com.gayatri.dentalclinic.repository.PatientRepository;
import com.gayatri.dentalclinic.repository.UserAccountRepository;
import com.gayatri.dentalclinic.repository.MedicalRecordRepository;
import com.gayatri.dentalclinic.security.CustomUserDetails;
import com.gayatri.dentalclinic.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AppointmentServiceImplTest {

    private AppointmentRepository appointmentRepository;
    private UserAccountRepository userAccountRepository;
    private AppointmentServiceImpl service;

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        PatientRepository patientRepository = mock(PatientRepository.class);
        DentistRepository dentistRepository = mock(DentistRepository.class);
        userAccountRepository = mock(UserAccountRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        service = new AppointmentServiceImpl(
                appointmentRepository,
                patientRepository,
                dentistRepository,
                userAccountRepository,
                notificationService,
                mock(MedicalRecordRepository.class)
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentDoctorAppointmentsReturnsOnlyTheAuthenticatedDoctorsAppointments() {
        Dentist currentDentist = Dentist.builder().id(10L).name("Dr. Riya Kapoor").build();
        UserAccount account = UserAccount.builder()
                .id(7L)
                .email("riya.kapoor@example.com")
                .passwordHash("hash")
                .role(Role.DOCTOR)
                .dentist(currentDentist)
                .build();
        Patient patient = Patient.builder()
                .id(3L)
                .firstName("Ava")
                .lastName("Sharma")
                .build();
        Appointment appointment = Appointment.builder()
                .id(25L)
                .patient(patient)
                .dentist(currentDentist)
                .appointmentDate(LocalDate.of(2026, 10, 12))
                .appointmentTime(LocalTime.of(10, 30))
                .status(AppointmentStatus.BOOKED)
                .remarks("Initial consultation")
                .build();

        authenticateAsDoctor(7L);
        when(userAccountRepository.findById(7L)).thenReturn(Optional.of(account));
        when(appointmentRepository.findByDentistIdOrderByAppointmentDateAscAppointmentTimeAsc(10L))
                .thenReturn(List.of(appointment));

        List<AppointmentResponseDto> result = service.getCurrentDoctorAppointments();

        assertEquals(1, result.size());
        assertEquals("Ava Sharma", result.getFirst().getPatientName());
        assertEquals(10L, result.getFirst().getDentistId());
        verify(appointmentRepository).findByDentistIdOrderByAppointmentDateAscAppointmentTimeAsc(10L);
    }

    @Test
    void getCurrentDoctorAppointmentsRejectsNonDoctorUsers() {
        CustomUserDetails patient = new CustomUserDetails(2L, "ava@example.com", "hash", Role.PATIENT, 4L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                patient,
                null,
                patient.getAuthorities()
        ));

        assertThrows(AccessDeniedException.class, () -> service.getCurrentDoctorAppointments());

        verifyNoInteractions(userAccountRepository, appointmentRepository);
    }

    private void authenticateAsDoctor(Long userId) {
        CustomUserDetails doctor = new CustomUserDetails(userId, "riya.kapoor@example.com", "hash", Role.DOCTOR, null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                doctor,
                null,
                doctor.getAuthorities()
        ));
    }
}
