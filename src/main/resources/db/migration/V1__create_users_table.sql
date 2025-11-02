CREATE TABLE users
(
    id                        BIGSERIAL PRIMARY KEY,
    username                  VARCHAR(50)  NOT NULL UNIQUE,
    email                     VARCHAR(255) NOT NULL UNIQUE,
    password                  VARCHAR(255) NOT NULL,
    server_domain             VARCHAR(255) NOT NULL, -- Add this
    created_at                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    failed_login_attempts     INTEGER      NOT NULL DEFAULT 0,
    last_failed_login_attempt TIMESTAMP,
    account_locked_until      TIMESTAMP
);

CREATE INDEX idx_username ON users (username);
CREATE INDEX idx_email ON users (email);
CREATE INDEX idx_server_domain ON users (server_domain);
CREATE INDEX idx_account_locked_until ON users (account_locked_until) WHERE account_locked_until IS NOT NULL;

CREATE UNIQUE INDEX idx_federated_user ON users (username, server_domain);