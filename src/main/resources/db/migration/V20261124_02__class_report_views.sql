-- #689 리포트 열람 기록. ddl-auto(update) 로도 테이블은 생기지만 운영 반영 기록을 위해 둔다. 재실행해도 안전하다.

CREATE TABLE IF NOT EXISTS class_report_views (
    id              BIGSERIAL PRIMARY KEY,
    class_id        BIGINT    NOT NULL,
    report_id       BIGINT    NOT NULL,
    user_id         BIGINT    NOT NULL,
    first_viewed_at TIMESTAMP NOT NULL,
    last_viewed_at  TIMESTAMP NOT NULL,
    view_count      INTEGER   NOT NULL,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    CONSTRAINT uk_class_report_views_report_user UNIQUE (report_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_class_report_views_class ON class_report_views (class_id);
