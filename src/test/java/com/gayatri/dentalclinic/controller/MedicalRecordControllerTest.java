package com.gayatri.dentalclinic.controller;

import com.gayatri.dentalclinic.entity.*;
import com.gayatri.dentalclinic.enums.AppointmentStatus;
import com.gayatri.dentalclinic.enums.Role;
import com.gayatri.dentalclinic.repository.*;
import com.gayatri.dentalclinic.security.CustomUserDetails;
import com.gayatri.dentalclinic.security.JwtUtil;
import com.gayatri.dentalclinic.service.MedicalRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Base64;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:medical-records;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
        "spring.jpa.open-in-view=false", "app.admin.email=", "app.admin.password="
})
@AutoConfigureMockMvc
class MedicalRecordControllerTest {
    private static final String PRESCRIPTION = """
            {"title":"Visit prescription","prescriptionText":"Medicine and dosage entered by doctor.\\nReview in one week.","notes":"Follow-up advice"}
            """;
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+afoQAAAAASUVORK5CYII=");

    @Autowired MockMvc mvc;
    @Autowired PatientRepository patients;
    @Autowired DentistRepository dentists;
    @Autowired UserAccountRepository accounts;
    @Autowired AppointmentRepository appointments;
    @Autowired MedicalRecordRepository records;
    @Autowired MedicalRecordFileRepository files;
    @Autowired JwtUtil jwt;
    @LocalServerPort int port;

    private Appointment visit;
    private Patient patient;
    private Patient otherPatient;
    private Dentist dentist;
    private Dentist otherDentist;
    private CustomUserDetails doctor;
    private CustomUserDetails otherDoctor;
    private CustomUserDetails patientUser;
    private CustomUserDetails otherPatientUser;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString();
        patient = patients.save(Patient.builder().firstName("Test").lastName("Patient").phone(unique + "1").build());
        otherPatient = patients.save(Patient.builder().firstName("Other").lastName("Patient").phone(unique + "2").build());
        dentist = dentists.save(Dentist.builder().name("Dr. Test").build());
        otherDentist = dentists.save(Dentist.builder().name("Dr. Other").build());
        doctor = CustomUserDetails.fromUserAccount(accounts.save(UserAccount.builder()
                .email(unique + "@doctor.example").passwordHash("unused").role(Role.DOCTOR).dentist(dentist).build()));
        otherDoctor = CustomUserDetails.fromUserAccount(accounts.save(UserAccount.builder()
                .email(unique + "@other.example").passwordHash("unused").role(Role.DOCTOR).dentist(otherDentist).build()));
        patientUser = CustomUserDetails.fromUserAccount(accounts.save(UserAccount.builder()
                .email(unique + "@patient.example").passwordHash("unused").role(Role.PATIENT).patient(patient).build()));
        otherPatientUser = new CustomUserDetails(10001L, "other@example.test", "unused", Role.PATIENT, otherPatient.getId());
        visit = appointments.save(Appointment.builder().patient(patient).dentist(dentist)
                .appointmentDate(LocalDate.of(2026, 1, 1)).appointmentTime(LocalTime.of(10, 30))
                .status(AppointmentStatus.COMPLETED).build());
    }

    @Test
    void realHttpFlowAuthenticatesSharesAndDownloadsTenMiBReportsAndRejectsOversize() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            var prescription = client.send(http(base() + "/prescription", doctor)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(PRESCRIPTION)).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(201, prescription.statusCode(), prescription.body());
            var text = client.send(http("/api/medical-records/" + latestId() + "/file", patientUser).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, text.statusCode());
            assertTrue(text.body().contains("Review in one week."));

            byte[] report = new byte[MedicalRecordService.MAX_FILE_BYTES];
            System.arraycopy(PNG, 0, report, 0, PNG.length);
            var upload = client.send(http(base() + "/documents", doctor)
                    .header("Content-Type", "multipart/form-data; boundary=medical-test")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody(report))).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(201, upload.statusCode(), upload.body());
            var download = client.send(http("/api/medical-records/" + latestId() + "/file", patientUser).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, download.statusCode());
            assertArrayEquals(report, download.body());
            var history = client.send(http("/api/medical-records/mine", patientUser).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, history.statusCode());
            assertTrue(history.body().contains("HTTP report"));

            var rejected = client.send(http(base() + "/documents", doctor)
                    .header("Content-Type", "multipart/form-data; boundary=medical-test")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody(new byte[report.length + 1]))).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(413, rejected.statusCode(), rejected.body());
            assertEquals(2, records.findByPatientIdOrderByCreatedAtDescIdDesc(patient.getId()).size());
        }
    }

    private HttpRequest.Builder http(String path, CustomUserDetails caller) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + jwt.generateToken(caller));
    }

    private byte[] multipartBody(byte[] bytes) throws Exception {
        var body = new ByteArrayOutputStream();
        body.write(("--medical-test\r\nContent-Disposition: form-data; name=\"title\"\r\n\r\nHTTP report\r\n"
                + "--medical-test\r\nContent-Disposition: form-data; name=\"type\"\r\n\r\nREPORT\r\n"
                + "--medical-test\r\nContent-Disposition: form-data; name=\"file\"; filename=\"report.png\"\r\n"
                + "Content-Type: image/png\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(bytes);
        body.write("\r\n--medical-test--\r\n".getBytes(StandardCharsets.UTF_8));
        return body.toByteArray();
    }

    @Test
    void doctorIssuesPrescriptionAndPatientCanListReadAndDownloadLater() throws Exception {
        Long id = prescribe();
        mvc.perform(get("/api/medical-records/mine").with(user(patientUser)))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].doctorName").value("Dr. Test"))
                .andExpect(jsonPath("$[0].appointmentDate").value("2026-01-01"));
        mvc.perform(get("/api/medical-records/{id}", id).with(user(patientUser)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.prescriptionText").value(containsString("Review in one week.")));
        mvc.perform(get("/api/medical-records/{id}/file", id).with(user(patientUser)))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/plain"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(containsString("Patient: Test Patient")))
                .andExpect(content().string(containsString("Follow-up advice")));
        mvc.perform(get("/api/appointments/{id}/medical-records", visit.getId()).with(user(doctor)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
    }

    @Test
    void sameTitleAndFileNameCanBeSharedMoreThanOnceForTheSameAppointment() throws Exception {
        prescribe();
        prescribe();
        for (int i = 0; i < 2; i++) {
            mvc.perform(multipart(base() + "/documents")
                            .file(new MockMultipartFile("file", "report.png", "image/png", PNG))
                            .param("title", "Follow-up report").param("type", "REPORT").with(user(doctor)))
                    .andExpect(status().isCreated());
            mvc.perform(get("/api/medical-records/{id}/file", latestId()).with(user(patientUser)))
                    .andExpect(status().isOk()).andExpect(content().bytes(PNG));
        }
        mvc.perform(get("/api/medical-records/mine").with(user(patientUser)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4));
        mvc.perform(get(base()).with(user(doctor)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void recordAndFileCannotBeReadByAnotherPatientDoctorAdminOrAnonymousCaller() throws Exception {
        Long id = prescribe();
        for (String suffix : new String[]{"", "/file"}) {
            String path = "/api/medical-records/" + id + suffix;
            mvc.perform(get(path).with(user(otherPatientUser))).andExpect(status().isForbidden());
            mvc.perform(get(path).with(user(otherDoctor))).andExpect(status().isForbidden());
            mvc.perform(get(path).with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
            mvc.perform(get(path)).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/medical-records/mine").with(user(otherPatientUser)))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/appointments/{id}/medical-records", visit.getId()).with(user(otherDoctor)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/medical-records/mine").with(user(new CustomUserDetails(2L, "orphan", "unused", Role.PATIENT, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadsPersistInDatabaseAndDownloadsMatchOriginalBytes() throws Exception {
        mvc.perform(multipart(base() + "/documents").file(new MockMultipartFile("file", "scan.png", "application/octet-stream", PNG))
                        .param("title", "Dental X-ray").param("type", "XRAY").param("notes", "Review at next visit").with(user(doctor)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.fileSize").value(PNG.length)).andExpect(jsonPath("$.fileName").value("scan.png"));
        Long id = latestId();
        assertEquals(PNG.length, files.findById(id).orElseThrow().getContent().length);
        mvc.perform(get("/api/medical-records/{id}/file", id).with(user(patientUser)))
                .andExpect(status().isOk()).andExpect(content().contentType("image/png")).andExpect(content().bytes(PNG));
        mvc.perform(get("/api/medical-records/{id}/file?download=false", id).with(user(doctor)))
                .andExpect(status().isOk()).andExpect(header().string("Content-Disposition", containsString("inline")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void pdfAndJpegCanBeUploadedIncludingAnExistingPrescription() throws Exception {
        for (MockMultipartFile file : new MockMultipartFile[]{
                new MockMultipartFile("file", "prescription.pdf", "application/pdf", "%PDF-1.4\n%%EOF".getBytes(StandardCharsets.US_ASCII)),
                new MockMultipartFile("file", "report.jpg", "image/jpeg", new byte[]{(byte) 255, (byte) 216, (byte) 255, (byte) 217})}) {
            mvc.perform(multipart(base() + "/documents").file(file).param("title", "Uploaded prescription").param("type", "PRESCRIPTION").with(user(doctor)))
                    .andExpect(status().isCreated());
            mvc.perform(get("/api/medical-records/{id}/file", latestId()).with(user(patientUser)))
                    .andExpect(status().isOk()).andExpect(content().bytes(file.getBytes()));
        }
    }

    @Test
    void rejectsEmptyOversizedDisguisedAndMismatchedFiles() throws Exception {
        for (MockMultipartFile file : new MockMultipartFile[]{
                new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]),
                new MockMultipartFile("file", "fake.pdf", "application/pdf", "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8)),
                new MockMultipartFile("file", "wrong.pdf", "application/pdf", PNG),
                new MockMultipartFile("file", "too-large.png", "image/png", new byte[MedicalRecordService.MAX_FILE_BYTES + 1])}) {
            mvc.perform(multipart(base() + "/documents").file(file).param("title", "Invalid").param("type", "REPORT").with(user(doctor)))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(0, records.findByPatientIdOrderByCreatedAtDescIdDesc(patient.getId()).size());
    }

    @Test
    void validatesRequiredAndMaximumLengthFields() throws Exception {
        mvc.perform(post(base() + "/prescription").with(user(doctor)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" \",\"prescriptionText\":\" \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(base() + "/prescription").with(user(doctor)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Prescription\",\"prescriptionText\":\"" + "a".repeat(12001) + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart(base() + "/documents").file(new MockMultipartFile("file", "scan.png", "image/png", PNG))
                        .param("title", "Report").param("type", "INVALID").with(user(doctor)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyAssignedDoctorCanWriteAndCancelledAppointmentsAreReadOnly() throws Exception {
        for (CustomUserDetails caller : new CustomUserDetails[]{patientUser, otherDoctor}) {
            mvc.perform(post(base() + "/prescription").with(user(caller)).contentType(MediaType.APPLICATION_JSON).content(PRESCRIPTION))
                    .andExpect(status().isForbidden());
            mvc.perform(multipart(base() + "/documents").file(new MockMultipartFile("file", "scan.png", "image/png", PNG))
                            .param("title", "X-ray").param("type", "XRAY").with(user(caller)))
                    .andExpect(status().isForbidden());
        }
        Long id = prescribe();
        visit.setStatus(AppointmentStatus.CANCELLED);
        appointments.save(visit);
        mvc.perform(post(base() + "/prescription").with(user(doctor)).contentType(MediaType.APPLICATION_JSON).content(PRESCRIPTION))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/medical-records/{id}/file", id).with(user(patientUser))).andExpect(status().isOk());
    }

    @Test
    void appointmentsWithRecordsCannotBeDeletedOrTransferredEvenByAdmin() throws Exception {
        Long id = prescribe();
        mvc.perform(delete("/api/appointments/{id}", visit.getId()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/appointments/{id}", visit.getId()).with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(appointmentBody(otherPatient.getId(), otherDentist.getId())))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/medical-records/{id}", id).with(user(patientUser)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.patientId").value(patient.getId()));
    }

    @Test
    void doctorsCannotTakeOverAnAppointmentThroughExistingAppointmentApi() throws Exception {
        mvc.perform(put("/api/appointments/{id}", visit.getId()).with(user(otherDoctor))
                        .contentType(MediaType.APPLICATION_JSON).content(appointmentBody(patient.getId(), otherDentist.getId())))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/appointments/{id}", visit.getId()).with(user(otherDoctor)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/appointments").with(user(otherDoctor))).andExpect(content().json("[]"));
    }

    private String base() { return "/api/appointments/" + visit.getId() + "/medical-records"; }

    private Long prescribe() throws Exception {
        mvc.perform(post(base() + "/prescription").with(user(doctor)).contentType(MediaType.APPLICATION_JSON).content(PRESCRIPTION))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.patientName").value("Test Patient"));
        return latestId();
    }

    private Long latestId() { return records.findByPatientIdOrderByCreatedAtDescIdDesc(patient.getId()).getFirst().getId(); }

    private String appointmentBody(Long patientId, Long dentistId) {
        return "{\"patientId\":" + patientId + ",\"dentistId\":" + dentistId
                + ",\"appointmentDate\":\"2026-01-01\",\"appointmentTime\":\"10:30\",\"status\":\"COMPLETED\"}";
    }
}
