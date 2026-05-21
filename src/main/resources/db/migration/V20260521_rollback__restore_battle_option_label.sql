-- Rollback for V20260521_01__drop_battle_option_label.sql.
-- Restore battle option labels from the option order before rolling back application code.

ALTER TABLE battle_options
    ADD COLUMN IF NOT EXISTS label VARCHAR(10);

WITH ordered_options AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY battle_id
            ORDER BY display_order, id
        ) AS option_order
    FROM battle_options
)
UPDATE battle_options bo
SET label = CASE oo.option_order
    WHEN 1 THEN 'A'
    WHEN 2 THEN 'B'
    WHEN 3 THEN 'C'
    WHEN 4 THEN 'D'
    ELSE NULL
END
FROM ordered_options oo
WHERE bo.id = oo.id
  AND bo.label IS NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM battle_options
        WHERE label IS NULL
    ) THEN
        RAISE EXCEPTION 'battle_options.label rollback failed: unsupported option count';
    END IF;
END $$;

ALTER TABLE battle_options
    ALTER COLUMN label SET NOT NULL;

DROP INDEX IF EXISTS idx_battle_options_battle_order;
