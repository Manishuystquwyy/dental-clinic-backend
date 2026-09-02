CREATE DATABASE IF NOT EXISTS dental_clinic
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'dental_app'@'%' IDENTIFIED BY 'change-this-password';

GRANT ALL PRIVILEGES ON dental_clinic.* TO 'dental_app'@'%';
FLUSH PRIVILEGES;
