# dental-clinic-backend

Spring Boot backend for Gayatri Dental Clinic.

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
