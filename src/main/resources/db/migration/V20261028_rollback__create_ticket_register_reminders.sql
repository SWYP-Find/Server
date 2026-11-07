-- Rollback for V20261028_03__create_ticket_register_reminders.sql
-- 애플리케이션 코드를 #589 이전으로 되돌린 뒤 실행할 것.
DROP TABLE IF EXISTS ticket_register_reminders;
