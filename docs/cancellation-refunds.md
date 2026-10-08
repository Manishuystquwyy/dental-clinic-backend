# Appointment cancellation and Razorpay refunds

Cancellation and refund processing have separate lifecycles. The cancellation transaction sets the appointment to `CANCELLED` and records a durable refund request for eligible payments. It commits before the background worker calls Razorpay. Availability queries exclude cancelled appointments, so the released slot can be booked as soon as that transaction commits, even when Razorpay is unavailable.

## Eligibility and patient experience

The existing `app.appointment.cancellation-cutoff-hours` policy controls online cancellation. Times are evaluated in the clinic's India time zone. Patients cannot cancel completed appointments. An accepted cancellation refunds the full successfully paid Razorpay consultation amount to the original payment method; the worker checks the captured payment at Razorpay before creating a refund. Unpaid or failed payments do not cause a money transfer. Payments collected outside Razorpay require clinic handling.

The appointment stays cancelled throughout refund processing. The patient sees the refund amount, current progress and refund reference in appointment history. The application does not report a successful refund merely because it accepted the cancellation or sent a refund request.

The existing checkout flow can record an authorized payment before capture. Such a cancellation remains pending while the worker waits for capture; the worker does not capture the payment merely to refund it. Razorpay can automatically refund an uncaptured authorization. An automatic provider refund or an independent Dashboard refund without the application's receipt requires clinic reconciliation rather than being assumed to be this refund request. See [Razorpay's refund eligibility](https://razorpay.com/docs/api/refunds/).

| Refund state | Meaning |
| --- | --- |
| `PENDING` | The refund request is saved and waiting for processing or a safe retry. |
| `REFUND_INITIATED` | Razorpay has accepted the refund and its reference is stored. |
| `REFUNDED` | A verified refund webhook or reconciliation lookup confirms Razorpay's final processed status. |
| `FAILED` | Automatic processing needs clinic attention. The appointment remains cancelled. |

Normal Razorpay refunds generally reach the original payment method in 5–7 working days. Razorpay's processed status confirms its refund processing; it does not promise that the patient's bank has already posted the credit. See [Razorpay's normal refund API](https://razorpay.com/docs/api/refunds/create-normal/).

## Razorpay webhook setup

Configure a public HTTPS webhook in the same Razorpay Test or Live mode as the application's API keys:

```text
https://YOUR_API_HOST/api/payments/razorpay/webhook
```

Subscribe to these events:

- `payment.captured`
- `refund.created`
- `refund.processed`
- `refund.failed`

Set the same secret in the Razorpay webhook configuration and `app.razorpay.webhook-secret` on the backend. API credentials and the webhook secret remain server side. Signature verification uses `X-Razorpay-Signature` and the exact raw request body before parsing. A localhost URL cannot receive Razorpay delivery directly; use a reachable staging or deployment endpoint for end-to-end testing.

Razorpay can deliver duplicate events and can deliver them out of order. Status updates must therefore be idempotent and must not move a completed refund back to a pending state. A webhook is only associated with the intended local payment/refund when its identifiers, amount and currency match. See [refund events](https://razorpay.com/docs/webhooks/refunds/) and [webhook verification, duplicates and ordering](https://razorpay.com/docs/webhooks/validate-test/).

## Deployment

For a database managed with explicit MySQL migrations, apply [`db/appointment-refunds.sql`](../db/appointment-refunds.sql) before starting the updated backend. Review the migration against the deployed schema and take the usual database backup first. Do not run the migration a second time after Hibernate has already added the same columns. Hibernate schema updates can create the additions for local development when `ddl-auto=update` is enabled.

Deploy the matching backend and frontend, configure webhook subscriptions, and verify the flow in Razorpay Test mode before enabling it for live cancellations. Existing SMTP configuration is used for patient notifications.

The migration adds the durable `refunds` table and `payments.currency`. Refund timestamps, including worker leases and submission attempts, represent UTC instants and use MySQL `DATETIME(6)` columns. It does not create refunds for historical cancellations: staff must first check whether those payments were already refunded in the Dashboard. The `INR` backfill assumes the deployment's historical payments used INR; verify that assumption before applying it to a database containing other currencies.

## Worker configuration

The backend periodically claims due refunds using database leases. It performs gateway calls outside the cancellation transaction. Multiple application instances can use the same database; a claimed refund's lease prevents routine concurrent processing, and the provider idempotency key protects retries when a worker crashes or a lease expires.

| Spring property | Default | Purpose |
| --- | --- | --- |
| `app.refunds.enabled` | `true` | Master switch for scheduling background refund work. |
| `app.refunds.worker-enabled` | `true` | Enables the background worker. |
| `app.refunds.poll-interval-ms` | `15000` | Interval between scans for due work. |
| `app.refunds.initial-delay-ms` | `15000` | Initial wait before the first scan. |
| `app.refunds.lease-seconds` | `180` | Duration of a worker claim. |

Transient errors use delayed retries with backoff, starting at 60 seconds and capped at one hour. Accepted refunds are reconciled after a 60-second delay while still awaiting completion. Setting either scheduling switch to `false` pauses initiation, reconciliation and retryable completion email work. Cancellation still commits a durable `PENDING` intent and incoming verified webhooks can update it. Re-enabling scheduling resumes due work; configure these switches deliberately when making operational changes.

## Recovery and operations

Refund intent is persisted with the cancellation rather than kept only in an in-memory queue. A missed worker wake-up or server restart does not lose the request. Reconciliation also fetches the provider state when webhook delivery is delayed or missed.

The same logical refund must use the same stored idempotency key and immutable request body across retries. Razorpay documents `X-Refund-Idempotency` for normal refunds; a network timeout does not justify creating a new key or reducing/recalculating the retry amount. A `409` for an in-progress request can be retried with that same key. The worker commits `submission_attempted_at` before its first POST, so recovery can repeat that identical request even if a receipt lookup has not yet caught up with provider state. See [Razorpay's idempotent normal refund API](https://razorpay.com/docs/api/refunds/normal-refunds-idempotent).

The refund ID from an accepted POST is saved as `REFUND_INITIATED`. Final completion is confirmed separately by a signed webhook or subsequent reconciliation read, including when the POST response itself already reports `processed`.

When the provider state conflicts with the local request, the amount has already been partly refunded elsewhere, or the provider rejects the refund with a definitive validation error, the request becomes `FAILED` and automatic submissions stop. Investigate the payment/refund in the Razorpay Dashboard before issuing any manual refund. Do not blindly change a local status to `REFUNDED`, delete a paid appointment/payment, or create a replacement refund key. Preserve the financial history and reconcile the known provider reference. Transient errors remain retryable with no arbitrary retry limit that abandons the patient's refund.

Useful release checks include cancellation response latency during a gateway outage, immediate slot availability, repeated cancellation requests, restart recovery, timeout recovery, duplicate and reordered webhooks, an uncaptured payment that is later captured, authorization checks, and a processed refund shown in appointment history.

## Patient email delivery

Refund completion email work is persisted with the final refund status. The worker snapshots the patient and refund information, sends using the existing SMTP settings, and records `notification_sent_at` only after a successful send. An SMTP failure can be retried without changing a completed refund or reopening the appointment.

This is retryable delivery, not a guarantee that an email appears in an inbox exactly once. A process crash after the SMTP server accepts the email but before the sent timestamp is committed can cause a repeated message on recovery. The website's appointment history remains the authoritative status for the patient.
