CREATE SCHEMA IF NOT EXISTS edge;

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR NOT NULL,
    password VARCHAR(120) NOT NULL,
    birth_date DATE,
    first_name VARCHAR,
    last_name VARCHAR,
    auth_provider VARCHAR,
    provider_id VARCHAR,
    created_ts TIMESTAMPTZ,
    last_modified_ts TIMESTAMPTZ
);

ALTER TABLE users
ADD CONSTRAINT uk_users_email UNIQUE (email);
