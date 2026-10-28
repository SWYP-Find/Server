-- Rollback for V20261028_02__classes_create_request_key.sql
-- 애플리케이션 코드를 #576 이전으로 되돌린 뒤 실행할 것.
ALTER TABLE classes DROP CONSTRAINT IF EXISTS uk_classes_owner_request_key;
ALTER TABLE classes DROP COLUMN IF EXISTS create_request_hash;
ALTER TABLE classes DROP COLUMN IF EXISTS create_request_key;
