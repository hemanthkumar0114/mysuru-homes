-- Run this once in MySQL Workbench (connect to your local instance as root,
-- open a new SQL tab, paste this whole file, click the lightning-bolt
-- "Execute" button). Creates the database and a dedicated app user so
-- Spring Boot never needs your root password. Safe to re-run.

CREATE DATABASE IF NOT EXISTS real_estate
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'realestate_app'@'localhost'
  IDENTIFIED BY 'REDACTED-OLD-DEV-PASSWORD';

GRANT ALL PRIVILEGES ON real_estate.* TO 'realestate_app'@'localhost';

FLUSH PRIVILEGES;

-- That's it. backend/src/main/resources/application.yml already points at
-- this user/password by default. Spring Boot creates all the tables itself
-- on first run (ddl-auto: update) - you don't need to write any CREATE
-- TABLE statements here.
