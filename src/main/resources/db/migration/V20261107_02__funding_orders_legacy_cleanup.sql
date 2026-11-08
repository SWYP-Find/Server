-- #616 운영 DB 에 남아 있는 이전 펀딩 스키마 정리.
-- 운영에는 v3.0.0 이전 펀딩 기능이 만든 funding_orders 가 있어 campaign_id 가 BIGINT(funding_campaigns FK)이고,
-- 지금 엔티티가 쓰지 않는 NOT NULL 컬럼(capacity, buyer_name, reward_line_id, reward_type, additional_support_amount)이 남아 있다.
-- 그대로 두면 주문 저장이 NOT NULL 위반으로 실패하고, ddl-auto 의 campaign_id VARCHAR(50) 변경도 FK 때문에 막힌다.
-- 지금 엔티티 기준(dev 와 같은 구조)으로 맞춘다. 멱등하게 작성되어 재실행해도 안전하다. 한 트랜잭션으로 실행한다.
-- 행이 있으면 아무것도 하지 않고 멈춘다 (이전 스키마로 쌓인 데이터를 지우지 않기 위해).

BEGIN;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM funding_orders) THEN
        RAISE EXCEPTION 'funding_orders 에 행이 있어 정리를 멈춥니다. 데이터를 먼저 확인하세요.';
    END IF;
END $$;

ALTER TABLE funding_orders DROP CONSTRAINT IF EXISTS fk_funding_order_campaign;
ALTER TABLE funding_orders DROP CONSTRAINT IF EXISTS uk_funding_order_external_line;
ALTER TABLE funding_orders DROP CONSTRAINT IF EXISTS uk_funding_order_supporter_line;
ALTER TABLE funding_orders DROP CONSTRAINT IF EXISTS funding_orders_capacity_check;
DROP INDEX IF EXISTS idx_funding_order_buyer_name;
DROP INDEX IF EXISTS idx_funding_order_contact_hash;

ALTER TABLE funding_orders ALTER COLUMN campaign_id TYPE VARCHAR(50) USING campaign_id::text;

ALTER TABLE funding_orders
    DROP COLUMN IF EXISTS external_order_id,
    DROP COLUMN IF EXISTS external_supporter_number,
    DROP COLUMN IF EXISTS reward_line_id,
    DROP COLUMN IF EXISTS reward_type,
    DROP COLUMN IF EXISTS capacity,
    DROP COLUMN IF EXISTS buyer_name,
    DROP COLUMN IF EXISTS buyer_contact_encrypted,
    DROP COLUMN IF EXISTS buyer_contact_search_hash,
    DROP COLUMN IF EXISTS source_version,
    DROP COLUMN IF EXISTS external_gift_text,
    DROP COLUMN IF EXISTS reward_amount,
    DROP COLUMN IF EXISTS additional_support_amount,
    DROP COLUMN IF EXISTS total_support_amount,
    DROP COLUMN IF EXISTS notification_requested,
    DROP COLUMN IF EXISTS tumblbug_notification_status,
    DROP COLUMN IF EXISTS tumblbug_payment_status,
    DROP COLUMN IF EXISTS tumblbug_gift_delivery_status,
    DROP COLUMN IF EXISTS source_snapshot;

COMMIT;
