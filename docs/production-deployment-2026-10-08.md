# Production deployment — 2026-10-08

Backend deployed to `ubuntu@130.210.24.123`, service `dental-clinic.service`,
API `https://api.gayatridental.com`. The frontend was not part of this deployment.

## Release and configuration

- Source: branch `feature/sonarfix`, commit `4105622`, plus the working tree's
  nonsecret property changes: multipart limits 10/11 MB, 5-second SMTP timeouts,
  `spring.jpa.open-in-view=false`, and 12-hour cancellation/rescheduling cutoffs.
- Built from an isolated sanitized copy at
  `/private/tmp/dental-backend-release-20261008`; original property edits were
  preserved in the repository.
- Database, SMTP, JWT, Razorpay, and administrator credentials require environment
  values in the deployed JAR. Existing effective production values were copied
  directly from the previous JAR into `/etc/dental-clinic.env` on the server,
  without printing credentials. That file is root-owned with mode `0600`.
- Artifact: `/opt/dental-clinic/app.jar`, owned by `ubuntu`, mode `0600`.
- SHA-256:
  `293dc222a8192caf441132d0f920d1d177d8299dcbec3f73bc0f26a4f1bcf2c8`.
- Existing production profile, systemd memory limits, Java 384 MiB maximum heap,
  Tomcat limits, Hikari pool settings, upload paths, and CORS values were retained.
- First startup at 10:27:05 UTC (15:57:05 IST); final service restart after
  worker authorization at 10:31:11 UTC (16:01:11 IST). Application startup
  completed at 10:32:22 UTC (16:02:22 IST).

## Backup and database migration

Rollback directory: `/opt/dental-clinic/backups/prod-20261008T102652Z/`, root-owned,
mode `0700`. It contains the previous JAR, environment file, service unit and
overrides, consistent compressed database dump, applied SQL, and release manifest.
The database dump is mode `0600` and was taken with the application stopped.

Added nullable `payments.currency` and the `refunds` table, indexes, unique keys,
and payment foreign key. The applied SQL follows `db/appointment-refunds.sql`,
with its currency default and historical INR backfill omitted. Checkout sessions
do not store historical currency, so existing payment currency values remain null.
The refund implementation uses its legacy INR fallback and verifies provider
currency before a transfer.

Existing row counts were preserved: 4 appointments, 4 payments, 2 patients, and
4 user accounts. Final schema has 13 application tables; `refunds` initially has
zero records. Existing uploads were preserved.

## Validation

- Sanitized release: `./mvnw -B clean verify`; 308 tests, 307 passed, 1 optional
  MySQL migration test skipped because separate test-database credentials were
  unavailable. Tests used fake process environment values, not live credentials.
- Local and public HTTPS `/api/dentists`: HTTP 200.
- Existing administrator login, `/api/auth/me`, and authenticated
  `/actuator/health`: HTTP 200; health status `UP`.
- `/api/appointments/policy`: HTTP 200, both cutoffs 12 hours.
- Authenticated `/api/appointments`, `/api/payments`, and `/api/bills`: HTTP 200,
  each returning the 4 existing records.
- Uploaded and installed artifact checksums match. Service is active with zero
  automatic restarts. Initial systemd memory sample: approximately 363 MiB;
  system available memory approximately 303 MiB.
- All API smoke checks and public HTTPS passed again after enabling the worker.
  The running process has `APP_REFUNDS_WORKER_ENABLED=true` and a scheduler
  thread. Final startup log has zero error/OOM lines; memory approximately 367 MiB.

Validation covered read-only application endpoints. Live gateway transfers and
webhook delivery were not exercised.

## Refund worker status

The first startup used `APP_REFUNDS_WORKER_ENABLED=false` for health verification.
Automatic approval review required explicit authorization because activation can
trigger irreversible transfers. The user then authorized "Enable automated live
refunds"; `APP_REFUNDS_WORKER_ENABLED=true` was applied and the service restarted.
The worker initiates and reconciles eligible live Razorpay refunds and retries
completion-email delivery. Historical cancellations were not backfilled.

The Razorpay Dashboard webhook subscriptions still need verification for
`payment.captured`, `refund.created`, `refund.processed`, and `refund.failed` at
`https://api.gayatridental.com/api/payments/razorpay/webhook`, using the existing
matching webhook secret. A deployment-time read-only provider lookup was rejected
by automatic approval review because it would send production payment identifiers
with live credentials; the migration instead preserves historical currency values.

## Rollback

Restore the previous artifact and environment using the backup above:

```bash
sudo systemctl stop dental-clinic.service
sudo install -o ubuntu -g ubuntu -m 600 /opt/dental-clinic/backups/prod-20261008T102652Z/app.jar /opt/dental-clinic/app.jar
sudo install -o root -g root -m 600 /opt/dental-clinic/backups/prod-20261008T102652Z/dental-clinic.env /etc/dental-clinic.env
sudo systemctl start dental-clinic.service
```

Leave additive schema and refund records intact. The prior application does not
process pending refunds. Restoring a database dump after live transfers can erase
reconciliation and idempotency history and cannot reverse provider transfers.
