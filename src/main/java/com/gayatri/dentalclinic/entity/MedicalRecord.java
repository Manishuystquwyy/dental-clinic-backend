package com.gayatri.dentalclinic.entity;

import com.gayatri.dentalclinic.enums.MedicalRecordType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "medical_records", indexes = {
        @Index(name = "idx_medical_records_patient", columnList = "patient_id,created_at"),
        @Index(name = "idx_medical_records_appointment", columnList = "appointment_id,created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, updatable = false)
    private Appointment appointment;

    // Ownership is captured when issued; changing an appointment must never move a patient's records.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false, updatable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dentist_id", nullable = false, updatable = false)
    private Dentist dentist;

    @Column(nullable = false, updatable = false)
    private String patientName;
    @Column(nullable = false, updatable = false)
    private String doctorName;
    @Column(nullable = false, updatable = false)
    private LocalDate appointmentDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MedicalRecordType type;
    @Column(nullable = false, length = 160)
    private String title;
    @Column(length = 2000)
    private String notes;
    @Column(columnDefinition = "TEXT")
    private String prescriptionText;
    @Column(length = 180)
    private String fileName;
    private String contentType;
    private Long fileSize;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
