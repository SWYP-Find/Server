DO $$
BEGIN
    IF to_regclass('public.battle_options') IS NULL THEN
        RETURN;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'battle_options'
          AND column_name = 'label'
    ) THEN
        UPDATE battle_options
        SET display_order = CASE label
            WHEN 'A' THEN 1
            WHEN 'B' THEN 2
            WHEN 'C' THEN 3
            WHEN 'D' THEN 4
            ELSE display_order
        END
        WHERE display_order IS NULL;
    END IF;

    WITH ranked_options AS (
        SELECT
            id,
            ROW_NUMBER() OVER (
                PARTITION BY battle_id
                ORDER BY COALESCE(display_order, 9999), id
            ) AS fallback_order
        FROM battle_options
    )
    UPDATE battle_options bo
    SET display_order = ranked_options.fallback_order
    FROM ranked_options
    WHERE bo.id = ranked_options.id
      AND bo.display_order IS NULL;

    ALTER TABLE battle_options ALTER COLUMN display_order SET NOT NULL;
    ALTER TABLE battle_options DROP COLUMN IF EXISTS label;

    CREATE INDEX IF NOT EXISTS idx_battle_options_battle_order
        ON battle_options (battle_id, display_order, id);
END $$;
