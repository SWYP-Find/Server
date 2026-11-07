-- #543 이용권 재발급: 주문당 이용권 행을 여러 개 허용(폐기 이력 보존)하되, 살아 있는(REVOKED 아닌) 행은 1개만.
-- JPA 엔티티의 @UniqueConstraint(funding_order_id) 는 제거했고, ddl-auto 는 기존 제약을 지우지 않으므로 수동 적용.
ALTER TABLE class_tickets DROP CONSTRAINT IF EXISTS uk_class_tickets_funding_order;

CREATE UNIQUE INDEX IF NOT EXISTS ux_class_tickets_live_order
    ON class_tickets (funding_order_id) WHERE status <> 'REVOKED';
