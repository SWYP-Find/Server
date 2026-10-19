-- Rollback for V20261019_02__class_tickets_reissue.sql
-- 재발급 기능을 되돌릴 때 실행. 데이터는 건드리지 않는다.
-- 주의: 재발급으로 한 주문에 REVOKED 이용권이 여러 개 쌓인 상태면 UK 복구가 실패한다.
--       그 경우 REVOKED 행을 먼저 정리(삭제)한 뒤 실행할 것.

DROP INDEX IF EXISTS ux_class_tickets_live_order;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_class_tickets_funding_order') THEN
        ALTER TABLE class_tickets ADD CONSTRAINT uk_class_tickets_funding_order UNIQUE (funding_order_id);
    END IF;
END $$;
