-- Rollback for V20261021_03__create_class_debate_schema.sql
-- 토론 대화가 모두 삭제된다. 애플리케이션 코드를 #548 이전으로 되돌린 뒤 실행할 것.
DROP TABLE IF EXISTS class_debate_messages;
DROP TABLE IF EXISTS class_debate_sessions;
