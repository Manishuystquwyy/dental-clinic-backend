package com.gayatri.dentalclinic.service.impl;

import com.gayatri.dentalclinic.dto.response.AppointmentResponseDto;
import com.gayatri.dentalclinic.dto.request.AppointmentRequestDto;
import com.gayatri.dentalclinic.exception.BadRequestException;
import com.gayatri.dentalclinic.service.BookingTime;
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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AppointmentServiceImplTest {

    private AppointmentRepository appointmentRepository;
    private UserAccountRepository userAccountRepository;
    private AppointmentServiceImpl service;
    private PatientRepository patientRepository;
    private DentistRepository dentistRepository;

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        patientRepository = mock(PatientRepository.class);
        dentistRepository = mock(DentistRepository.class);
        userAccountRepository = mock(UserAccountRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        service = new AppointmentServiceImpl(
                appointmentRepository,
                patientRepository,
                dentistRepository,
                userAccountRepository,
                notificationService,
                mock(MedicalRecordRepository.class),
                new BookingTime(Clock.fixed(Instant.parse("2026-09-24T08:30:00Z"), ZoneOffset.UTC))
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

    @Test
    void availabilityExcludesPastDatesAndTimesIncludingTheCurrentSlot() {
        when(dentistRepository.findById(10L)).thenReturn(Optional.of(Dentist.builder().id(10L).build()));

        assertEquals(List.of(), service.getAvailability(10L, LocalDate.of(2026, 9, 23)).getAvailableSlots());
        assertEquals(List.of(LocalTime.of(14, 30), LocalTime.of(15, 30), LocalTime.of(16, 0),
                LocalTime.of(16, 30), LocalTime.of(17, 0), LocalTime.of(17, 30), LocalTime.of(18, 0)),
                service.getAvailability(10L, LocalDate.of(2026, 9, 24)).getAvailableSlots());
        assertEquals(10, service.getAvailability(10L, LocalDate.of(2026, 9, 25)).getAvailableSlots().size());
    }

    @Test
    void futureAvailabilityStillExcludesBookedSlots() {
        LocalDate date = LocalDate.of(2026, 9, 25);
        when(dentistRepository.findById(10L)).thenReturn(Optional.of(Dentist.builder().id(10L).build()));
        when(appointmentRepository.findByDentistIdAndAppointmentDateAndStatusIn(
                10L, date, List.of(AppointmentStatus.BOOKED, AppointmentStatus.COMPLETED)))
                .thenReturn(List.of(Appointment.builder().appointmentTime(LocalTime.of(11, 0)).build()));
        var slots = service.getAvailability(10L, date).getAvailableSlots();
        assertEquals(9, slots.size());
        assertEquals(false, slots.contains(LocalTime.of(11, 0)));
    }

    @Test
    void createRejectsPastDatePastTimeAndCurrentTimeBeforeSaving() {
        stubPatientAndDentist();
        for (var request : List.of(
                request(LocalDate.of(2026, 9, 23), LocalTime.of(18, 0), AppointmentStatus.BOOKED),
                request(LocalDate.of(2026, 9, 24), LocalTime.of(11, 0), AppointmentStatus.BOOKED),
                request(LocalDate.of(2026, 9, 24), LocalTime.of(14, 0), AppointmentStatus.BOOKED))) {
            assertThrows(BadRequestException.class, () -> service.createAppointment(request));
        }
        verifyNoInteractions(appointmentRepository);
    }

    @Test
    void futureAppointmentCanBeCreated() {
        stubPatientAndDentist();
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(call -> call.getArgument(0));
        var result = service.createAppointment(request(LocalDate.of(2026, 9, 24),
                LocalTime.of(14, 30), AppointmentStatus.BOOKED));
        assertEquals(LocalTime.of(14, 30), result.getAppointmentTime());
    }

    @Test
    void reschedulingIntoThePastAndReopeningAnExpiredBookingAreRejected() {
        stubPatientAndDentist();
        var appointment = existingAppointment(AppointmentStatus.CANCELLED);
        when(appointmentRepository.findWithLockById(25L)).thenReturn(Optional.of(appointment));
        assertThrows(BadRequestException.class, () -> service.updateAppointment(25L,
                request(appointment.getAppointmentDate(), appointment.getAppointmentTime(), AppointmentStatus.BOOKED)));
        assertThrows(BadRequestException.class, () -> service.updateAppointment(25L,
                request(LocalDate.of(2026, 9, 24), LocalTime.of(11, 0), AppointmentStatus.BOOKED)));
    }

    @Test
    void existingPastAppointmentsCanStillBeCompletedOrCancelled() {
        stubPatientAndDentist();
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(call -> call.getArgument(0));
        for (var status : List.of(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED)) {
            var appointment = existingAppointment(AppointmentStatus.BOOKED);
            when(appointmentRepository.findWithLockById(25L)).thenReturn(Optional.of(appointment));
            assertEquals(status, service.updateAppointment(25L,
                    request(appointment.getAppointmentDate(), appointment.getAppointmentTime(), status)).getStatus());
        }
    }

    private AppointmentRequestDto request(LocalDate date, LocalTime time, AppointmentStatus status) {
        return new AppointmentRequestDto(3L, 10L, date, time, status, null);
    }

    private Appointment existingAppointment(AppointmentStatus status) {
        return Appointment.builder().id(25L).patient(Patient.builder().id(3L).build())
                .dentist(Dentist.builder().id(10L).build()).appointmentDate(LocalDate.of(2026, 9, 23))
                .appointmentTime(LocalTime.of(10, 30)).status(status).build();
    }

    private void stubPatientAndDentist() {
        when(patientRepository.findById(3L)).thenReturn(Optional.of(Patient.builder().id(3L).build()));
        when(dentistRepository.findWithLockById(10L)).thenReturn(Optional.of(Dentist.builder().id(10L).build()));
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
