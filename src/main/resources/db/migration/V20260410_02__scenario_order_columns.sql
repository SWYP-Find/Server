-- Stabilize scenario ordering for nodes/scripts/options

ALTER TABLE scenario_nodes
    ADD COLUMN IF NOT EXISTS node_order INTEGER;

ALTER TABLE scenario_scripts
    ADD COLUMN IF NOT EXISTS script_order INTEGER;

ALTER TABLE scenario_options
    ADD COLUMN IF NOT EXISTS option_order INTEGER;

WITH ordered AS (
    SELECT
        id,
        ROW_NUMBER() OVER (PARTITION BY scenario_id ORDER BY created_at ASC, id ASC) - 1 AS rn
    FROM scenario_nodes
)
UPDATE scenario_nodes sn
SET node_order = ordered.rn
FROM ordered
WHERE sn.id = ordered.id
  AND (sn.node_order IS NULL OR sn.node_order <> ordered.rn);

WITH ordered AS (
    SELECT
        id,
        ROW_NUMBER() OVER (PARTITION BY node_id ORDER BY created_at ASC, id ASC) - 1 AS rn
    FROM scenario_scripts
)
UPDATE scenario_scripts ss
SET script_order = ordered.rn
FROM ordered
WHERE ss.id = ordered.id
  AND (ss.script_order IS NULL OR ss.script_order <> ordered.rn);

WITH ordered AS (
    SELECT
        id,
        ROW_NUMBER() OVER (PARTITION BY node_id ORDER BY created_at ASC, id ASC) - 1 AS rn
    FROM scenario_options
)
UPDATE scenario_options so
SET option_order = ordered.rn
FROM ordered
WHERE so.id = ordered.id
  AND (so.option_order IS NULL OR so.option_order <> ordered.rn);

