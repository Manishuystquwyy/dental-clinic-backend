package com.gayatri.dentalclinic.dto.response;

import com.gayatri.dentalclinic.enums.MedicalRecordType;
import java.time.Instant;
import java.time.LocalDate;

public record MedicalRecordResponseDto(
        Long id, Long appointmentId, Long patientId, String patientName,
        Long dentistId, String doctorName, LocalDate appointmentDate,
        MedicalRecordType type, String title, String notes, String prescriptionText,
        String fileName, String contentType, Long fileSize, Instant createdAt
) {
}
