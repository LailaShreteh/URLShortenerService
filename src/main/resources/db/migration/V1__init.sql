-- Users = system of record (Postgres)
CREATE EXTENSION IF NOT EXISTS citext;

CREATE TABLE IF NOT EXISTS users (
  id            BIGSERIAL PRIMARY KEY,
  email         CITEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  status        SMALLINT NOT NULL DEFAULT 1  -- 1=active, 2=disabled
);

-- Short URLs
CREATE TABLE IF NOT EXISTS short_urls (
  code        VARCHAR(10) PRIMARY KEY,
  long_url    TEXT NOT NULL,
  user_id     BIGINT REFERENCES users(id),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  expires_at  TIMESTAMPTZ,
  is_custom   BOOLEAN NOT NULL DEFAULT FALSE,
  status      SMALLINT NOT NULL DEFAULT 1,   -- 1=active, 2=expired, 3=deleted
  url_hash    VARCHAR(64)                    -- optional: for idempotency lookups
);

CREATE INDEX IF NOT EXISTS idx_short_urls_user    ON short_urls(user_id);
CREATE INDEX IF NOT EXISTS idx_short_urls_expires ON short_urls(expires_at);

-- Idempotency
CREATE UNIQUE INDEX IF NOT EXISTS uq_short_urls_url_hash ON short_urls(url_hash);