-- Rollback for V20261119_03__create_ticket_code_attempts.sql
-- 애플리케이션 코드를 #653 이전으로 되돌린 뒤 실행할 것.
DROP TABLE IF EXISTS ticket_code_attempts;
