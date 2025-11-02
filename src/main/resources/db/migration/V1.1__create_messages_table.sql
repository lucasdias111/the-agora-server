CREATE TABLE chat_messages
(
    id                     BIGSERIAL PRIMARY KEY,
    from_user_id           BIGINT        NOT NULL,
    to_user_id             BIGINT        NOT NULL,
    from_user_server       VARCHAR(255),
    to_user_server         VARCHAR(255),
    message                VARCHAR(5000) NOT NULL,
    is_read                BOOLEAN   DEFAULT FALSE,
    is_edited              BOOLEAN   DEFAULT FALSE,
    created_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    federated_message_id   VARCHAR(512)
);

CREATE INDEX idx_from_user_id ON chat_messages (from_user_id);
CREATE INDEX idx_to_user_id ON chat_messages (to_user_id);
CREATE INDEX idx_created_at ON chat_messages (created_at);

CREATE INDEX idx_from_user_server ON chat_messages (from_user_server);
CREATE INDEX idx_to_user_server ON chat_messages (to_user_server);
CREATE INDEX idx_federated_message_id ON chat_messages (federated_message_id);

CREATE INDEX idx_user_server_composite ON chat_messages (to_user_id, to_user_server);