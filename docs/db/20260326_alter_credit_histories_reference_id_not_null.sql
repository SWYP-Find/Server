DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM credit_histories
        WHERE reference_id IS NULL
    ) THEN
        RAISE EXCEPTION 'credit_histories.reference_id contains NULL values. Backfill the rows before applying NOT NULL.';
    END IF;
END $$;

ALTER TABLE credit_histories
    ALTER COLUMN reference_id SET NOT NULL;
