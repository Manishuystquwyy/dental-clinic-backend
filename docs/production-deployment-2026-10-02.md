# Production deployment — 2026-10-02

Target: `ubuntu@130.210.24.123`, service `dental-clinic.service`, API
`https://api.gayatridental.com`. Java 21; 956 MiB RAM and 2 GiB swap.

## Runtime configuration

- Artifact: `/opt/dental-clinic/app.jar`; active profile `prod`.
- Environment: `/etc/dental-clinic.env` (root-only).
- Systemd override: `/etc/systemd/system/dental-clinic.service.d/40-small-vm.conf`.
  A copy is saved in `deploy/40-small-vm.conf`; it assumes this server's existing
  service user, paths, environment file, and restart policy.
- Heap: 64–384 MiB; Serial GC; direct buffers limited to 32 MiB and code cache
  to 96 MiB. Metaspace, native allocations and thread stacks require additional RAM.
- Systemd memory pressure threshold 650 MiB and hard limit 768 MiB. The existing
  `Restart=always` policy recovers after process exit, including JVM OOM exit.
- Tomcat: 25 worker threads, 3 spare threads, 100 connections, backlog 25.
- Hikari: maximum 3 connections, minimum idle 1, 5-second acquisition timeout,
  15-minute max lifetime.
- GC logs rotate under `/opt/dental-clinic/logs/` (4 archived files of 4 MiB
  plus the active file). Journald limits: 64 MiB persistent, 32 MiB runtime.
- Nginx request limit: 11 MiB, matching the backend multipart request limit.
- Firmware maintenance stays enabled as explicitly requested. Swap is retained.

## Database and source changes

The configured MySQL database `dental_clinic` on `10.0.0.96` had no application
tables before deployment. The user explicitly approved initializing it as a fresh
database using the existing Hibernate schema-update configuration. No historical
clinic data was restored.

Removed the optional `spring.datasource.hikari.pool-name` property from the backend
and mirrored production properties after the production JVM failed to bind it to
an already-started Hikari pool. The new H2-backed production-profile startup test
checks application startup and configured pool bounds; H2 did not reproduce the
original MySQL/JVM-specific binding failure, so live verification is also required.

Local validation: `./mvnw -B clean verify`, 179 tests, 178 passed, one optional
MySQL migration test skipped because its separate test-database credentials were
not configured.

Final artifact SHA-256:
`ac0662d9b6a96b31083d61ae3339c1c5810e10abb77983f65f4587b0d47fcfc2`.

## Verified deployment result

- Corrected deployment started at 16:43:05 UTC; application startup took 72 seconds.
- MySQL contains 12 application tables and one bootstrapped administrator.
- Administrator login and `/api/auth/me`: HTTP 200. Authenticated
  `/actuator/health`: HTTP 200 with `UP`.
- Public HTTPS checked from both the VM and local workstation: HTTP 200.
  Python urllib received an edge 403 while curl succeeded; no edge policy was changed.
- Thirty HTTPS read-only requests at concurrency three: all HTTP 200, p95 0.877 s,
  maximum 0.999 s. This checks an empty database and is not a representative booking,
  payment, upload, or sustained-load benchmark.
- Final sample: JVM RSS 339 MiB, zero JVM swap, 26 threads, 263 MiB system memory
  available, and zero restarts since the corrected deployment. No application
  errors or OOM messages appeared in that deployment's logs during verification.
- Removed four obsolete JAR copies (about 282 MiB), retained the immediate rollback
  artifact and all non-JAR recovery records. Journals fell from 232 MiB to 24 MiB.
  About 490 MiB of old files were removed; after retaining the new rollback copy,
  net disk usage fell by roughly 420 MiB. `/opt/dental-clinic` is now about 143 MiB.
- Existing uploads were preserved. Firmware daemon remains active and its refresh
  timer enabled; the backend remains enabled to start at boot.

## Rollback

The previous artifact, environment file, base unit, original memory override and
Nginx site are retained in `/opt/dental-clinic/backups/prod-20261002/` (root-only
directory). This rollback restores application/configuration files only, not the
database. The old deployment already had database query failures before this work.

To restore the previous application configuration, stop `dental-clinic`, copy the
backed-up `app.jar` and `dental-clinic.env` to their original paths, restore the
backed-up `20-memory.conf`, and remove the added `40-small-vm.conf` override. Run
`systemctl daemon-reload` and start `dental-clinic`. Restore the saved Nginx site only
if needed, then run `nginx -t` before reloading Nginx.

## Operational checks

Use `systemctl status dental-clinic`, `journalctl -u dental-clinic`, `free -m`,
`vmstat 1 5`, and the rotating GC logs. Check `/api/dentists` over HTTPS for a public
database-backed smoke test; `/actuator/health` requires authentication.

Memory limits reduce resource risk but cannot guarantee operation under arbitrary
traffic. During startup the VM reported roughly 76% CPU steal in a short sample;
CPU contention can slow startup and requests independently of JVM memory use.
