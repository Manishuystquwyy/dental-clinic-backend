-- Run against the application's MySQL database before deploying database-backed records.
-- Hibernate ddl-auto=update does not remove constraints on unmapped legacy columns.
-- Preserve legacy object references; only relax NOT NULL on the retired storage fields.
-- Safe to rerun, including on a fresh schema where these columns do not exist.
SET @medical_records_legacy_alter = (
    SELECT CONCAT('ALTER TABLE `medical_records` ', GROUP_CONCAT(
        CONCAT('MODIFY COLUMN `', COLUMN_NAME, '` ', COLUMN_TYPE, ' NULL DEFAULT NULL')
        ORDER BY ORDINAL_POSITION SEPARATOR ', '
    ))
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'medical_records'
      AND COLUMN_NAME IN ('object_key', 'storage_bucket', 'storage_namespace')
      AND IS_NULLABLE = 'NO'
);
SET @medical_records_legacy_alter = COALESCE(@medical_records_legacy_alter, 'SELECT 1');
PREPARE medical_records_legacy_statement FROM @medical_records_legacy_alter;
EXECUTE medical_records_legacy_statement;
DEALLOCATE PREPARE medical_records_legacy_statement;
