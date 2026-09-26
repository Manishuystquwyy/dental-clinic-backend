# Hibernate findings reviewed

## Fetch plans and transaction boundaries

All to-one mappings default to LAZY. Appointment list queries explicitly fetch the
patient needed for patientName, so listing appointments does not issue a patient
query per appointment. Login fetches its patient details explicitly without an
outer authentication transaction: failed-login audit records and account blocking
must still commit when login raises an exception.

Open Session in View is disabled in the common application configuration. Services
assemble response DTOs within their transactions when they traverse lazy state.
Bill, payment and treatment writes have explicit write transactions; their reads
use read-only transactions. Existing checkout locking and transaction boundaries
are retained.

## Monetary mappings (HIB-MAP-014)

Bill totals/discount, payment amount and treatment cost explicitly declare
DECIMAL(38,2). This preserves Hibernate's previously generated numeric(38,2)
mapping (confirmed in the baseline integration-test DDL), rather than narrowing
existing money columns. Dentist fees and checkout amounts retain DECIMAL(10,2).
These annotations do not introduce a new input-validation or rounding policy.

No shared database was altered by this change. Before deploying against a schema
managed independently of Hibernate, compare its five columns using:

```sql
SELECT TABLE_NAME, COLUMN_NAME, NUMERIC_PRECISION, NUMERIC_SCALE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND ((TABLE_NAME = 'bills' AND COLUMN_NAME IN ('total_amount', 'discount', 'final_amount'))
    OR (TABLE_NAME = 'payments' AND COLUMN_NAME = 'amount')
    OR (TABLE_NAME = 'treatment' AND COLUMN_NAME = 'cost'));
```

## Retained identifier trade-off (HIB-ID-001): 11 findings

The dev and prod profiles use MySQL. Retain IDENTITY for the existing AUTO_INCREMENT
Long primary keys in Patient, Payment, PublicRequest, UserAccount, Appointment,
RazorpayCheckoutSession, MedicalRecord, Bill, Dentist, Treatment and LoginAttempt.
The current flows create individual records and small related groups. No measured
bulk-insert bottleneck justifies changing the ID strategy and migrating existing
primary/foreign keys or introducing a table-backed sequence allocator.

IDENTITY prevents JDBC insert batching for these entities. Revisit this decision
if a bulk import/write workload needs batching, with measurements and a database
migration plan. The BootUI review prompts remain visible; they are not suppressed.

References:
- https://docs.hibernate.org/orm/7.2/userguide/html_single/#identifiers-generators-identity
- https://dev.mysql.com/doc/refman/8.4/en/example-auto-increment.html
