# Prescriptions and patient documents

## Doctor workflow

1. Sign in as a doctor and open **My Dashboard**.
2. Select **Prescriptions & reports** on an assigned appointment. Use **All visits / past appointments** for an earlier or completed visit.
3. Select **Write prescription**, enter a title, medicine names, dosage, frequency, duration and instructions, and optional follow-up advice.
4. Alternatively, select **Upload document / X-ray**, choose Prescription, X-ray, Report or Document, and attach a PDF, PNG or JPEG up to 10 MiB. DICOM, Word and other formats must first be exported to one of these formats.
5. **Save & share with patient** publishes immediately to that patient's dashboard. A handwritten/scanned prescription can be uploaded as a Prescription.

Shared records are append-only. To correct a prescription, add a new record identifying the earlier record and the correction. Cancelled appointments allow viewing existing records but do not accept new records.

## Patient workflow

Open **My Dashboard → My prescriptions & reports**. Records from all visits appear newest first and can be filtered by type. **View** opens a prescription or document preview; **Download** saves the original uploaded file or a UTF-8 text copy of a written prescription. A written prescription also has **Print / Save as PDF**, which uses the browser's print dialog. Patients can refresh the list after a doctor shares a record.

## Storage and access

- `medical_records` stores the original patient, issuing doctor, appointment, names and visit date, record details and creation time.
- `medical_record_files` stores uploaded bytes in a separate `LONGBLOB`, so listing records does not load file data. File and metadata writes use one database transaction.
- Medical documents are never stored in the public `/uploads` directory. Every list, detail and file request requires a bearer token and server-side ownership checks. Only the owning patient and issuing doctor can read a record. Admin accounts do not have medical-record access.
- Doctor writes derive the patient and doctor from the assigned appointment; the client cannot supply ownership. Appointment updates cannot transfer records to another patient or doctor, and appointments with records cannot be deleted. The file response uses `Cache-Control: no-store`, a safe filename, an explicit content type and sandbox headers.
- Uploads are restricted by size, file signature and matching extension. PDF previews use PDF.js canvas rendering; no scripting manager or interactive annotation layer is created. Original files remain downloadable, including documents that cannot be previewed. This is not a malware-scanning service or a DICOM viewer.

## Deployment

Deploy both frontend and backend together. With the repository's existing `spring.jpa.hibernate.ddl-auto=update` configuration, Hibernate creates the two new tables. For environments managed with explicit schema migrations, apply `db/medical-records.sql` before deployment instead.

For databases that previously used object storage, also run `db/medical-records-legacy-storage.sql` against the application database. This migration makes the retired `object_key`, `storage_bucket`, and `storage_namespace` columns optional, without deleting their values or existing records. It is safe to rerun and does nothing when those columns are absent. Hibernate's schema update and `CREATE TABLE IF NOT EXISTS` do not remove their old `NOT NULL` constraints. Without this migration, new prescriptions and documents fail with MySQL error 1364 (missing default value), which older backend versions incorrectly display as “A record with the same unique value already exists”. This migration does not copy historical object-store files into database storage.

The application limits each file to 10 MiB and multipart requests to 11 MiB. Configure the reverse proxy/load balancer to allow at least 11 MiB (`client_max_body_size 11m;` for an Nginx API location), and ensure the database's `max_allowed_packet` accommodates the upload plus statement overhead (at least 16 MiB).

Back up both medical-record tables with the rest of MySQL and include them in restore checks. Database disk usage grows with uploaded documents; this implementation does not require a filesystem volume for these files. Production persistence depends on using the durable production database, not an in-memory development database.

## API

| Method | Path | Caller | Purpose |
| --- | --- | --- | --- |
| GET | `/api/medical-records/mine` | Patient | Own history |
| GET | `/api/appointments/{id}/medical-records` | Assigned doctor | Shared records for a visit |
| POST | `/api/appointments/{id}/medical-records/prescription` | Assigned doctor | JSON: `title`, `prescriptionText`, optional `notes` |
| POST | `/api/appointments/{id}/medical-records/documents` | Assigned doctor | Multipart: `file`, `title`, `type`, optional `notes` |
| GET | `/api/medical-records/{id}` | Owning patient / issuing doctor | Record details |
| GET | `/api/medical-records/{id}/file` | Owning patient / issuing doctor | Download; optional `download=false` for inline disposition |

Record types: `PRESCRIPTION`, `XRAY`, `REPORT`, `OTHER`. Titles are limited to 160 characters, prescription text to 12,000, and notes to 2,000. No public file URL is returned.

## Verification

`./mvnw test` includes integration tests for issuing, listing and downloading prescriptions, file byte persistence, cross-patient and cross-doctor access denial, role restrictions, upload validation, cancelled appointments and protection against appointment reassignment/deletion.

The medical-record suite also sends authenticated requests through a real HTTP server to verify prescription sharing, patient downloads, 10 MiB multipart uploads and oversized-file rejection. Repeated titles and file names are allowed for a visit.

The optional `MedicalRecordMigrationMySqlTest` verifies the legacy schema failure and repair using MySQL. Set `TEST_MYSQL_URL`, `TEST_MYSQL_USERNAME` and `TEST_MYSQL_PASSWORD` in the environment, then run `./mvnw -Dtest=MedicalRecordMigrationMySqlTest test`. The database user must be able to create and drop a temporary database; the test uses a unique `medical_records_test_...` database and removes it afterwards, without changing application tables. It checks preservation of old references, repeated migration execution and fresh-schema compatibility. Without these environment variables this test is skipped.
