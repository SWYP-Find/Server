-- Rollback for V20261021_02__classes_topic_version.sql
-- 애플리케이션 코드를 #547 이전으로 되돌린 뒤 실행할 것. 커스텀 배틀 클래스는 주제 연결 정보를 잃는다.
ALTER TABLE classes DROP CONSTRAINT IF EXISTS uk_classes_topic_draft;
ALTER TABLE classes DROP COLUMN IF EXISTS topic_draft_id;
ALTER TABLE classes DROP COLUMN IF EXISTS topic_version_id;
