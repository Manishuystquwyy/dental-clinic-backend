# Checkout and payment dates

The clinic uses `Asia/Kolkata` (IST). The server and database may remain on UTC.

- `razorpay_checkout_sessions.created_at` and `updated_at` are local IST values in MySQL `DATETIME` columns. Use `BookingTime.now()` when writing them; these columns do not contain a timezone offset.
- `payments.payment_date` for Razorpay payments is the payment entity's `created_at` Unix timestamp converted to an IST calendar date. A delayed webhook must retain that payment date rather than using its delivery date.
- `bills.bill_date` uses the IST date when the bill is created.
- Appointment dates and times already represent clinic time; do not shift them.

For example, `2026-09-24T19:59:53Z` corresponds to `2026-09-25 01:29:53` IST and payment date `2026-09-25`.

## Existing production data

The previous checkout implementation used the host's UTC clock for timezone-less timestamps and date-only payment fields. The September 2026 correction uses a fixed before/after plan, verifies payment dates against Razorpay, and compares each original database value before updating. It must not be repeated by blindly adding 330 minutes.

The production backup and correction plan are under `/opt/dental-clinic/backups/timezone-fix-20260925/`. The application change and historical data correction are deployed together while checkout writes are stopped. Other tables' timestamp conventions are outside this correction.
