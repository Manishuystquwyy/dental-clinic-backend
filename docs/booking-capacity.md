# Booking latency and server capacity

## Confirmation delivery

Both direct and Razorpay bookings use `NotificationServiceImpl`. It captures recipient,
body and appointment ID while entities are managed, and registers an `afterCommit`
callback with the current transaction (including an outer payment transaction). Only
successful commit submits SMTP work to the dedicated `appointment-mail-*` executor.
Rollback and commit failure do not submit work. The callback does no network or DB I/O;
the worker has no JPA entities or transaction. SMTP cannot delay connection cleanup or
hold the dentist row lock. Nontransactional calls also use the worker.

The queue holds at most 100 messages and two workers send concurrently by default.
Existing SMTP connection/read/write timeouts remain 5 seconds each. On saturation,
shutdown rejection, or SMTP failure, log entries include the appointment ID; the
booking remains successful. Delivery is best effort: queued messages can be lost on
process failure, and failed/rejected messages are not automatically retried. If durable
delivery is required, use a transactional outbox with a retrying dispatcher. Normal
shutdown drains the queue for up to 30 seconds; this does not guarantee a full drain.

Spring documents the commit callback and resource lifecycle here:
https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/support/TransactionSynchronization.html

## Database pool and request limits

The production profile now defaults to a maximum of 10 connections per backend
instance (previously 5), minimum idle 2, acquisition timeout 5 seconds (previously 30),
validation timeout 2 seconds, idle timeout 5 minutes and max lifetime 30 minutes.
These are an initial load-test configuration, not measured optimal sizing. Configure
max lifetime below any database/proxy connection lifetime imposed by your deployment.
Pool exhaustion now fails sooner; check booking errors alongside latency.

Tomcat is bounded to 50 request threads, 5 spare threads, 200 connections and a backlog
of 50. Mail uses its own bounded executor. All pool/concurrency limits are environment
overridable in `application-prod.properties`; mirrored deployment property files in
the parent workspace have the same defaults. Existing environment overrides win, so
update them when deploying. Use `SPRING_PROFILES_ACTIVE=prod` for these limits.

For N backend replicas, budget N × maximum-pool-size plus migration, admin and other
client connections below MySQL's `max_connections`. Increasing the pool cannot improve
throughput for bookings serialized on the same dentist row. Hikari recommends sizing
against measured database capacity:
https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing

## Server resources and deployment

No live server capacity was inspected or resized by this change. The systemd and
environment examples in `deploy/` provide a reproducible starting point. The example
heap budget is 128–768 MiB for a dedicated VM with at least 2 GiB RAM; the JVM also
needs native memory, thread stacks and metaspace, and the OS needs headroom. Adapt
the budget if MySQL or other services share the host. Do not use this example unchanged
on a 1 GiB instance. A larger heap alone cannot fix CPU saturation.

Before deployment, record `nproc`, `free -h`, `vmstat 1`, the current Java RSS and
`journalctl -u dental-clinic-backend` OOM/restart history. On MySQL, inspect
`max_connections`, `Threads_connected`, `Threads_running` and lock waits. During a
representative booking load test, compare p95/p99 request latency, booking failures,
Hikari active/pending/acquisition time, CPU utilization, RSS and GC pauses. Use existing
monitoring or a secured local Actuator connection; do not expose metrics publicly.
Test contention on a single dentist as well as bookings across different dentists.

If CPU remains saturated after shortening transactions, increase compute capacity.
If memory pressure, swapping or OOM restarts persist, increase VM RAM or reduce
concurrency/heap within a measured budget. If pool pending rises while DB CPU and lock
waits remain low, experiment with small pool increases; otherwise fix the DB bottleneck.
Capacity changes in OCI must be applied separately after confirming the current shape,
service placement and cost constraints.

For a new systemd installation:

1. Create the `dental-clinic` service user/group and `/opt/dental-clinic`, writable by
   that user for configured uploads. Copy the built JAR to `/opt/dental-clinic/backend.jar`.
2. Merge the example environment into `/etc/dental-clinic/backend.env`, preserving
   existing payment, mail, admin and frontend configuration and setting real credentials.
   Restrict this file to root. Ensure Java 21 is installed at `/usr/bin/java`.
3. Install the unit in `/etc/systemd/system/`, run `systemctl daemon-reload`, then
   `systemctl enable --now dental-clinic-backend`. For an existing service, merge settings
   into its current unit instead of starting a second backend.
4. Check service health and logs, then run a controlled booking and email test. Compare
   the load metrics above before increasing traffic.

Rollback: restore the previous JAR/config and restart the service. Explicitly setting
`SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=5` restores the previous pool ceiling while
retaining the email fix. Database schema changes are not part of this work.
