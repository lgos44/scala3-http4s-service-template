CREATE TABLE public.users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR NOT NULL UNIQUE,
    username VARCHAR NOT NULL UNIQUE,
    password VARCHAR NOT NULL,
    first_name VARCHAR,
    last_name VARCHAR,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);