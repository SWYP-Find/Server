-- Rollback for V20261022_01__classes_purged_at.sql
-- 애플리케이션 코드를 #565 이전으로 되돌린 뒤 실행할 것. 이미 파기된 데이터는 복구되지 않는다.
ALTER TABLE classes DROP COLUMN IF EXISTS purged_at;
