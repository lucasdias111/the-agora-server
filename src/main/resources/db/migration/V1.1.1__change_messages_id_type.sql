DROP INDEX IF EXISTS idx_from_user_id;
DROP INDEX IF EXISTS idx_to_user_id;

ALTER TABLE chat_messages
    ALTER COLUMN from_user_id TYPE BIGINT USING from_user_id::BIGINT,
    ALTER COLUMN to_user_id TYPE BIGINT USING to_user_id::BIGINT;

CREATE INDEX idx_from_user_id ON chat_messages (from_user_id);
CREATE INDEX idx_to_user_id ON chat_messages (to_user_id);