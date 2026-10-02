-- Apply once before deployment when Hibernate schema updates are disabled.
ALTER TABLE user_accounts ADD COLUMN google_subject VARCHAR(255) NULL;
CREATE UNIQUE INDEX uk_user_accounts_google_subject ON user_accounts (google_subject);
