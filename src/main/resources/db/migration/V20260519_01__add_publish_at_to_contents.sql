ALTER TABLE battles
    ADD COLUMN IF NOT EXISTS publish_at TIMESTAMP;

ALTER TABLE quizzes
    ADD COLUMN IF NOT EXISTS publish_at TIMESTAMP;

ALTER TABLE poll_contents
    ADD COLUMN IF NOT EXISTS publish_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_battles_status_publish_at
    ON battles (status, publish_at)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_quizzes_status_publish_at
    ON quizzes (status, publish_at);

CREATE INDEX IF NOT EXISTS idx_poll_contents_status_publish_at
    ON poll_contents (status, publish_at);
