# dental-clinic-backend

Spring Boot backend for Gayatri Dental Clinic.

## Prescriptions and patient documents

Doctors can write prescriptions and upload private patient documents from their dashboard. Patients can view and download their history. See [the workflow, API and deployment guide](docs/medical-records.md).

## Oracle Cloud MySQL

For Oracle Cloud Free Tier database setup, use the OCI MySQL HeatWave profile and guide:

- `src/main/resources/application-oci.properties`
- `.env.oracle.example`
- `db/oci-mysql-init.sql`
- `docs/oracle-cloud-mysql-setup.md`

## Ubuntu Deployment

Deployment helpers are in `deploy/`:

- `deploy/dental-clinic-backend.service`
- `deploy/dental-clinic-backend.env.example`
- `deploy/deploy-to-ubuntu.sh`
