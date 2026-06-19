-- display_order가 NULL인 battle_options 백필: 같은 battle_id 내 id 오름차순으로 1, 2 배정
UPDATE battle_options bo
SET display_order = sub.row_num
FROM (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY battle_id ORDER BY id) AS row_num
    FROM battle_options
    WHERE display_order IS NULL
) sub
WHERE bo.id = sub.id;

ALTER TABLE battle_options ALTER COLUMN display_order SET NOT NULL;
