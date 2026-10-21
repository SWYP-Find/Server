-- Rollback for V20261021_01__create_class_topic_schema.sql
-- 주제 초안·버전 데이터가 모두 삭제된다. 애플리케이션 코드를 #545 이전으로 되돌린 뒤 실행할 것.
DROP INDEX IF EXISTS idx_ai_jobs_ticket_type_started;
DROP TABLE IF EXISTS class_topic_versions;
DROP TABLE IF EXISTS class_topic_drafts;
