-- Apply after V20261019 class schema and V20261021 topic schema.
-- Extend origin/dev with optional metadata; keep its order/ticket lifecycle authoritative.
-- Optional metadata preserves masked code/suffix search for newly issued codes.
ALTER TABLE class_tickets ADD COLUMN IF NOT EXISTS code_suffix VARCHAR(4);
ALTER TABLE ticket_delivery_attempts ALTER COLUMN channel TYPE VARCHAR(32);
CREATE TABLE IF NOT EXISTS funding_campaigns (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    platform VARCHAR(20) NOT NULL,
    external_campaign_id VARCHAR(120) NOT NULL,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    register_policy_version VARCHAR(30) NOT NULL DEFAULT '1',
    CONSTRAINT uk_funding_campaign_platform_external UNIQUE (platform, external_campaign_id)
);
CREATE INDEX IF NOT EXISTS idx_funding_campaign_status ON funding_campaigns(status);

CREATE TABLE IF NOT EXISTS tumblbug_import_records (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    campaign_id BIGINT NOT NULL REFERENCES funding_campaigns(id),
    funding_order_id BIGINT NOT NULL REFERENCES funding_orders(id) ON DELETE CASCADE,
    supporter_number VARCHAR(120) NOT NULL,
    reward_line_id VARCHAR(120) NOT NULL,
    source_version VARCHAR(50),
    source_snapshot TEXT NOT NULL,
    delivery_recorded_ticket_id BIGINT,
    CONSTRAINT uk_tumblbug_supporter_line UNIQUE (campaign_id, supporter_number, reward_line_id),
    CONSTRAINT uk_tumblbug_funding_order UNIQUE (funding_order_id)
);

CREATE TABLE IF NOT EXISTS funding_admin_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    admin_user_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT NOT NULL,
    reason VARCHAR(500),
    metadata TEXT
);
CREATE INDEX IF NOT EXISTS idx_funding_audit_target_created
    ON funding_admin_audit_logs(target_type, target_id, created_at);
