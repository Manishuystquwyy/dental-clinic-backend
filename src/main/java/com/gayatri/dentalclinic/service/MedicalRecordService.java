package com.gayatri.dentalclinic.service;

import com.gayatri.dentalclinic.dto.request.MedicalDocumentRequestDto;
import com.gayatri.dentalclinic.dto.request.PrescriptionRequestDto;
import com.gayatri.dentalclinic.dto.response.MedicalRecordResponseDto;
import com.gayatri.dentalclinic.entity.*;
import com.gayatri.dentalclinic.enums.AppointmentStatus;
import com.gayatri.dentalclinic.enums.MedicalRecordType;
import com.gayatri.dentalclinic.enums.Role;
import com.gayatri.dentalclinic.exception.BadRequestException;
import com.gayatri.dentalclinic.exception.NotFoundException;
import com.gayatri.dentalclinic.repository.*;
import com.gayatri.dentalclinic.security.CustomUserDetails;
import com.gayatri.dentalclinic.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicalRecordService {
    public static final int MAX_FILE_BYTES = 10 * 1024 * 1024;
    private final MedicalRecordRepository records;
    private final MedicalRecordFileRepository files;
    private final AppointmentRepository appointments;
    private final UserAccountRepository accounts;

    public List<MedicalRecordResponseDto> getMyRecords() {
        CustomUserDetails user = requireRole(Role.PATIENT);
        if (user.getPatientId() == null) throw new AccessDeniedException("Patient profile is required.");
        return records.findByPatientIdOrderByCreatedAtDescIdDesc(user.getPatientId()).stream().map(this::toDto).toList();
    }

    public List<MedicalRecordResponseDto> getAppointmentRecords(Long appointmentId) {
        Long dentistId = currentDentistId();
        doctorAppointment(appointmentId, dentistId);
        return records.findByAppointmentIdAndDentistIdOrderByCreatedAtDescIdDesc(appointmentId, dentistId)
                .stream().map(this::toDto).toList();
    }

    public MedicalRecordResponseDto getRecord(Long id) {
        return toDto(authorizedRecord(id));
    }

    @Transactional
    public MedicalRecordResponseDto prescribe(Long appointmentId, PrescriptionRequestDto request) {
        Appointment appointment = writableAppointment(appointmentId);
        MedicalRecord record = newRecord(appointment, MedicalRecordType.PRESCRIPTION, request.title(), request.notes());
        record.setPrescriptionText(request.prescriptionText().trim());
        return toDto(records.save(record));
    }

    @Transactional
    public MedicalRecordResponseDto upload(Long appointmentId, MedicalDocumentRequestDto request, MultipartFile file) {
        Appointment appointment = writableAppointment(appointmentId);
        if (file == null || file.isEmpty()) throw new BadRequestException("Choose a non-empty file to upload.");
        if (file.getSize() > MAX_FILE_BYTES) throw new BadRequestException("Files must be 10 MB or smaller.");
        byte[] content;
        try (var input = file.getInputStream()) {
            content = input.readNBytes(MAX_FILE_BYTES + 1);
        } catch (IOException ex) {
            throw new BadRequestException("Unable to read the uploaded file. Please try again.");
        }
        if (content.length > MAX_FILE_BYTES) throw new BadRequestException("Files must be 10 MB or smaller.");
        String contentType = detectContentType(content);
        String name = safeFileName(file.getOriginalFilename(), contentType);
        MedicalRecord record = newRecord(appointment, request.getType(), request.getTitle(), request.getNotes());
        record.setFileName(name);
        record.setContentType(contentType);
        record.setFileSize((long) content.length);
        records.save(record);
        MedicalRecordFile storedFile = new MedicalRecordFile();
        storedFile.setRecord(record);
        storedFile.setContent(content);
        files.save(storedFile);
        return toDto(record);
    }

    public RecordDownload download(Long id) {
        MedicalRecord record = authorizedRecord(id);
        if (record.getFileName() != null) {
            MedicalRecordFile file = files.findById(id)
                    .orElseThrow(() -> new NotFoundException("Document file not found."));
            return new RecordDownload(record.getFileName(), record.getContentType(), file.getContent());
        }
        String text = "Gayatri Dental Clinic\nPrescription\n\n"
                + "Record: " + record.getId() + "\nPatient: " + record.getPatientName()
                + "\nDoctor: " + record.getDoctorName() + "\nVisit: " + record.getAppointmentDate()
                + "\nIssued: " + record.getCreatedAt() + "\n\n" + record.getTitle()
                + "\n\n" + record.getPrescriptionText()
                + (record.getNotes() == null ? "" : "\n\nAdditional advice / follow-up\n" + record.getNotes()) + "\n";
        return new RecordDownload("prescription-" + id + ".txt", "text/plain;charset=UTF-8", text.getBytes(StandardCharsets.UTF_8));
    }

    private MedicalRecord authorizedRecord(Long id) {
        CustomUserDetails user = SecurityUtils.getCurrentUser();
        if (user == null) throw new AccessDeniedException("Sign in to view medical records.");
        MedicalRecord record = records.findById(id).orElseThrow(() -> new NotFoundException("Medical record not found."));
        boolean allowed = user.getRole() == Role.PATIENT && user.getPatientId() != null
                && user.getPatientId().equals(record.getPatient().getId());
        if (user.getRole() == Role.DOCTOR) allowed = currentDentistId().equals(record.getDentist().getId());
        if (!allowed) throw new AccessDeniedException("You cannot access this medical record.");
        return record;
    }

    private CustomUserDetails requireRole(Role role) {
        CustomUserDetails user = SecurityUtils.getCurrentUser();
        if (user == null || user.getRole() != role) throw new AccessDeniedException("You cannot perform this action.");
        return user;
    }

    private Long currentDentistId() {
        CustomUserDetails user = requireRole(Role.DOCTOR);
        UserAccount account = accounts.findById(user.getId())
                .orElseThrow(() -> new AccessDeniedException("Doctor account not found."));
        if (account.getRole() != Role.DOCTOR || account.getDentist() == null) {
            throw new AccessDeniedException("A linked doctor profile is required.");
        }
        return account.getDentist().getId();
    }

    private Appointment doctorAppointment(Long id, Long dentistId) {
        Appointment appointment = appointments.findById(id).orElseThrow(() -> new NotFoundException("Appointment not found."));
        if (appointment.getDentist() == null || !Objects.equals(dentistId, appointment.getDentist().getId())) {
            throw new AccessDeniedException("You can only manage records for your own appointments.");
        }
        return appointment;
    }

    private Appointment writableAppointment(Long id) {
        Long dentistId = currentDentistId();
        Appointment appointment = appointments.findWithLockById(id)
                .orElseThrow(() -> new NotFoundException("Appointment not found."));
        if (appointment.getDentist() == null || !dentistId.equals(appointment.getDentist().getId())) {
            throw new AccessDeniedException("You can only manage records for your own appointments.");
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BadRequestException("Records cannot be added to a cancelled appointment.");
        }
        return appointment;
    }

    private MedicalRecord newRecord(Appointment appointment, MedicalRecordType type, String title, String notes) {
        return MedicalRecord.builder().appointment(appointment).patient(appointment.getPatient()).dentist(appointment.getDentist())
                .patientName(appointment.getPatient().getFirstName() + " " + appointment.getPatient().getLastName())
                .doctorName(appointment.getDentist().getName()).appointmentDate(appointment.getAppointmentDate())
                .type(type).title(title.trim()).notes(notes == null || notes.isBlank() ? null : notes.trim())
                .createdAt(Instant.now()).build();
    }

    private String detectContentType(byte[] bytes) {
        if (bytes.length >= 5 && new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")) return "application/pdf";
        if (bytes.length >= 8 && (bytes[0] & 255) == 137 && bytes[1] == 80 && bytes[2] == 78 && bytes[3] == 71
                && bytes[4] == 13 && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10) return "image/png";
        if (bytes.length >= 3 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216 && (bytes[2] & 255) == 255) return "image/jpeg";
        throw new BadRequestException("Only PDF, PNG and JPEG files are supported. Export X-rays in one of these formats.");
    }

    private String safeFileName(String original, String type) {
        String name = original == null ? "document" : original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}\\p{Cf}]", "").trim();
        String lower = name.toLowerCase(Locale.ROOT);
        boolean extensionMatches = switch (type) {
            case "application/pdf" -> lower.endsWith(".pdf");
            case "image/png" -> lower.endsWith(".png");
            default -> lower.endsWith(".jpg") || lower.endsWith(".jpeg");
        };
        if (!extensionMatches) throw new BadRequestException("The file extension must match its PDF, PNG or JPEG content.");
        if (name.length() > 180) name = name.substring(name.length() - 180);
        return name;
    }

    private MedicalRecordResponseDto toDto(MedicalRecord record) {
        return new MedicalRecordResponseDto(record.getId(), record.getAppointment().getId(), record.getPatient().getId(),
                record.getPatientName(), record.getDentist().getId(), record.getDoctorName(), record.getAppointmentDate(),
                record.getType(), record.getTitle(), record.getNotes(), record.getPrescriptionText(),
                record.getFileName(), record.getContentType(), record.getFileSize(), record.getCreatedAt());
    }

    public record RecordDownload(String fileName, String contentType, byte[] content) {}
}
