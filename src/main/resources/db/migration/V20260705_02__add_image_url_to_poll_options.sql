-- Add image_url column to poll_options so pre-vote (사전투표) options can show a philosopher icon,
-- matching the existing battle_options.image_url column.
ALTER TABLE poll_options ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);
