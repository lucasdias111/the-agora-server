
CREATE TABLE chat_messages
(
    id           BIGSERIAL PRIMARY KEY,
    from_user_id VARCHAR(255)  NOT NULL,
    to_user_id   VARCHAR(255)  NOT NULL,
    message      VARCHAR(5000) NOT NULL,
    is_read      BOOLEAN DEFAULT FALSE,
    is_edited    BOOLEAN DEFAULT FALSE,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_from_user_id ON chat_messages (from_user_id);
CREATE INDEX idx_to_user_id ON chat_messages (to_user_id);
CREATE INDEX idx_created_at ON chat_messages (created_at);
