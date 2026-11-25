-- =============================================================
-- Events App — Database Setup Script
-- =============================================================
-- Run this script once before starting the application:
--   mysql -u root -p < database_setup.sql
-- Or paste it into MySQL Workbench / phpMyAdmin / DBeaver.
-- =============================================================

-- Create and select the database
CREATE DATABASE IF NOT EXISTS events_app;
USE events_app;


-- ----------------------------------------------------------------
-- Table: users
-- Stores registered user accounts.
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id            INT          AUTO_INCREMENT PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL
);


-- ----------------------------------------------------------------
-- Table: categories
-- Each event belongs to one category (one-to-many relationship).
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
    id   INT          AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

-- Default categories
INSERT INTO categories (name) VALUES
    ('Work'),
    ('Personal'),
    ('Social'),
    ('Sport'),
    ('Other');


-- ----------------------------------------------------------------
-- Table: events
-- Main table. References both users and categories via foreign keys.
--
-- Relationships:
--   events.user_id     → users.id      (many events per user)
--   events.category_id → categories.id (many events per category)
--
-- SQL JOIN example (used in EventDAO):
--   SELECT e.*, c.name AS category_name
--   FROM events e
--   JOIN categories c ON e.category_id = c.id
--   WHERE e.user_id = 1
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS events (
    id          INT          AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    event_date  DATE,
    user_id     INT          NOT NULL,
    category_id INT          NOT NULL,
    FOREIGN KEY (user_id)     REFERENCES users(id)      ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT
);


-- ----------------------------------------------------------------
-- Verify setup
-- ----------------------------------------------------------------
SELECT 'users'      AS table_name, COUNT(*) AS row_count FROM users
UNION ALL
SELECT 'categories' AS table_name, COUNT(*) AS row_count FROM categories
UNION ALL
SELECT 'events'     AS table_name, COUNT(*) AS row_count FROM events;