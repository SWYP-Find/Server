-- #547 검증된 주제로 커스텀 배틀 클래스 생성 (docs/erd/class.puml 기준)
-- 멱등하게 작성되어 재실행해도 안전하다.
ALTER TABLE classes ADD COLUMN IF NOT EXISTS topic_version_id BIGINT;
ALTER TABLE classes ADD COLUMN IF NOT EXISTS topic_draft_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_classes_topic_version') THEN
        ALTER TABLE classes ADD CONSTRAINT fk_classes_topic_version
            FOREIGN KEY (topic_version_id) REFERENCES class_topic_versions(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_classes_topic_draft') THEN
        ALTER TABLE classes ADD CONSTRAINT fk_classes_topic_draft
            FOREIGN KEY (topic_draft_id) REFERENCES class_topic_drafts(id);
    END IF;
    -- 같은 초안으로는 클래스 1개 (PRD §16 draft_id 고유). NULL(TTS 클래스)은 여러 행 허용
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_classes_topic_draft') THEN
        ALTER TABLE classes ADD CONSTRAINT uk_classes_topic_draft UNIQUE (topic_draft_id);
    END IF;
END $$;
