-- Apply only when using explicit MySQL schema migrations instead of Hibernate ddl-auto=update.
CREATE TABLE IF NOT EXISTS medical_records (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    appointment_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    dentist_id BIGINT NOT NULL,
    patient_name VARCHAR(255) NOT NULL,
    doctor_name VARCHAR(255) NOT NULL,
    appointment_date DATE NOT NULL,
    type ENUM('PRESCRIPTION', 'XRAY', 'REPORT', 'OTHER') NOT NULL,
    title VARCHAR(160) NOT NULL,
    notes VARCHAR(2000),
    prescription_text TEXT,
    file_name VARCHAR(180),
    content_type VARCHAR(255),
    file_size BIGINT,
    created_at DATETIME(6) NOT NULL,
    INDEX idx_medical_records_patient (patient_id, created_at),
    INDEX idx_medical_records_appointment (appointment_id, created_at),
    CONSTRAINT fk_medical_record_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    CONSTRAINT fk_medical_record_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_medical_record_dentist FOREIGN KEY (dentist_id) REFERENCES dentists(id)
);

CREATE TABLE IF NOT EXISTS medical_record_files (
    record_id BIGINT NOT NULL PRIMARY KEY,
    content LONGBLOB NOT NULL,
    CONSTRAINT fk_medical_record_file FOREIGN KEY (record_id) REFERENCES medical_records(id)
);
