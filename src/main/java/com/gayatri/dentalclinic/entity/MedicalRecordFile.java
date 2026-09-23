package com.gayatri.dentalclinic.entity;

import jakarta.persistence.*;
import lombok.*;

// Keep large file data out of history/list queries. Files are private and backed up with the database.
@Entity
@Table(name = "medical_record_files")
@Getter
@Setter
@NoArgsConstructor
public class MedicalRecordFile {
    @Id
    private Long id;
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "record_id")
    private MedicalRecord record;
    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] content;
}
