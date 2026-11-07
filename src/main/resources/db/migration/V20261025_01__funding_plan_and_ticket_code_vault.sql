ALTER TABLE funding_campaigns
    ADD COLUMN IF NOT EXISTS tumblbug_plan VARCHAR(20) NOT NULL DEFAULT 'RUN';

-- New codes are decryptable only by the application with TICKET_CODE_ENCRYPTION_KEY.
-- Existing rows have only a one-way hash and intentionally remain null.
ALTER TABLE class_tickets
    ADD COLUMN IF NOT EXISTS encrypted_code VARCHAR(160);

-- One funding order can produce one code per purchased quantity.
DROP INDEX IF EXISTS ux_class_tickets_live_order;
ALTER TABLE class_tickets DROP CONSTRAINT IF EXISTS uk_class_tickets_funding_order;
