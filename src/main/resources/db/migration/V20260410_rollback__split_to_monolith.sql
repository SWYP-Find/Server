-- Rollback helper: split schema -> monolith schema
-- NOTE: This script is idempotent-oriented and defensive for partially migrated DBs.

-- 1) Ensure legacy columns exist
DO $$
BEGIN
    IF to_regclass('public.battles') IS NOT NULL THEN
        ALTER TABLE battles ADD COLUMN IF NOT EXISTS type VARCHAR(20);
        ALTER TABLE battles ADD COLUMN IF NOT EXISTS title_prefix VARCHAR(200);
        ALTER TABLE battles ADD COLUMN IF NOT EXISTS title_suffix VARCHAR(200);
        ALTER TABLE battles ADD COLUMN IF NOT EXISTS item_a VARCHAR(255);
        ALTER TABLE battles ADD COLUMN IF NOT EXISTS item_a_desc TEXT;
        ALTER TABLE battles ADD COLUMN IF NOT EXISTS item_b VARCHAR(255);
        ALTER TABLE battles ADD COLUMN IF NOT EXISTS item_b_desc TEXT;
    END IF;

    IF to_regclass('public.battle_options') IS NOT NULL THEN
        ALTER TABLE battle_options ADD COLUMN IF NOT EXISTS quote TEXT;
        ALTER TABLE battle_options ADD COLUMN IF NOT EXISTS is_correct BOOLEAN DEFAULT FALSE;
    END IF;
END $$;

-- 2) Recreate battles rows from quizzes/polls
INSERT INTO battles (
    id, title, summary, description, thumbnail_url, type,
    title_prefix, title_suffix, target_date, audio_duration,
    status, creator_type, creator_id, view_count, total_participants,
    is_editor_pick, comment_count, deleted_at, created_at, updated_at
)
SELECT
    q.id,
    q.title,
    '왼쪽과 오른쪽 중 정답을 선택하세요',
    NULL,
    NULL,
    'QUIZ',
    NULL,
    NULL,
    q.target_date,
    NULL,
    CASE WHEN q.status IN ('PENDING','PUBLISHED','ARCHIVED','REJECTED') THEN q.status ELSE 'ARCHIVED' END,
    'ADMIN',
    NULL,
    0,
    COALESCE(q.total_participants_count, 0),
    FALSE,
    0,
    NULL,
    q.created_at,
    q.updated_at
FROM quizzes q
WHERE to_regclass('public.quizzes') IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM battles b WHERE b.id = q.id);

INSERT INTO battles (
    id, title, summary, description, thumbnail_url, type,
    title_prefix, title_suffix, target_date, audio_duration,
    status, creator_type, creator_id, view_count, total_participants,
    is_editor_pick, comment_count, deleted_at, created_at, updated_at
)
SELECT
    p.id,
    TRIM(CONCAT(COALESCE(p.title_prefix, ''), ' ', COALESCE(p.title_suffix, ''))),
    '빈칸에 들어갈 가장 적절한 답을 골라주세요',
    NULL,
    NULL,
    'POLL',
    p.title_prefix,
    p.title_suffix,
    p.target_date,
    NULL,
    CASE WHEN p.status IN ('PENDING','PUBLISHED','ARCHIVED','REJECTED') THEN p.status ELSE 'ARCHIVED' END,
    'ADMIN',
    NULL,
    0,
    COALESCE(p.total_participants_count, 0),
    FALSE,
    0,
    NULL,
    p.created_at,
    p.updated_at
FROM poll_contents p
WHERE to_regclass('public.poll_contents') IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM battles b WHERE b.id = p.id);

-- 3) Recreate battle_options rows from quiz/poll options
INSERT INTO battle_options (
    id, battle_id, label, title, stance, representative, quote,
    vote_count, is_correct, image_url, display_order, created_at, updated_at
)
SELECT
    qo.id,
    qo.quiz_id,
    qo.label,
    qo.text,
    NULL,
    NULL,
    qo.detail_text,
    0,
    COALESCE(qo.is_correct, FALSE),
    NULL,
    qo.display_order,
    qo.created_at,
    qo.updated_at
FROM quiz_options qo
WHERE to_regclass('public.quiz_options') IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM battle_options bo WHERE bo.id = qo.id);

INSERT INTO battle_options (
    id, battle_id, label, title, stance, representative, quote,
    vote_count, is_correct, image_url, display_order, created_at, updated_at
)
SELECT
    po.id,
    po.poll_id,
    po.label,
    po.title,
    NULL,
    NULL,
    NULL,
    COALESCE(po.vote_count, 0),
    FALSE,
    NULL,
    po.display_order,
    po.created_at,
    po.updated_at
FROM poll_options po
WHERE to_regclass('public.poll_options') IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM battle_options bo WHERE bo.id = po.id);

-- 4) Restore item_a/item_b style fields for monolith compatibility
UPDATE battles b
SET item_a = (
        SELECT qo.text
        FROM quiz_options qo
        WHERE qo.quiz_id = b.id AND qo.label = 'A'
        ORDER BY qo.display_order ASC, qo.id ASC
        LIMIT 1
    ),
    item_a_desc = (
        SELECT qo.detail_text
        FROM quiz_options qo
        WHERE qo.quiz_id = b.id AND qo.label = 'A'
        ORDER BY qo.display_order ASC, qo.id ASC
        LIMIT 1
    ),
    item_b = (
        SELECT qo.text
        FROM quiz_options qo
        WHERE qo.quiz_id = b.id AND qo.label = 'B'
        ORDER BY qo.display_order ASC, qo.id ASC
        LIMIT 1
    ),
    item_b_desc = (
        SELECT qo.detail_text
        FROM quiz_options qo
        WHERE qo.quiz_id = b.id AND qo.label = 'B'
        ORDER BY qo.display_order ASC, qo.id ASC
        LIMIT 1
    )
WHERE b.type = 'QUIZ'
  AND to_regclass('public.quiz_options') IS NOT NULL;

UPDATE battles b
SET item_a = (
        SELECT po.title
        FROM poll_options po
        WHERE po.poll_id = b.id AND po.label = 'A'
        ORDER BY po.display_order ASC, po.id ASC
        LIMIT 1
    ),
    item_b = (
        SELECT po.title
        FROM poll_options po
        WHERE po.poll_id = b.id AND po.label = 'B'
        ORDER BY po.display_order ASC, po.id ASC
        LIMIT 1
    )
WHERE b.type = 'POLL'
  AND to_regclass('public.poll_options') IS NOT NULL;

-- 5) Merge quiz/poll votes back into legacy votes table
INSERT INTO votes (
    user_id, battle_id, pre_vote_option_id, post_vote_option_id,
    is_tts_listened, created_at, updated_at
)
SELECT
    qv.user_id,
    qv.quiz_id,
    qv.option_id,
    qv.option_id,
    FALSE,
    qv.created_at,
    qv.updated_at
FROM quiz_user_votes qv
WHERE to_regclass('public.quiz_user_votes') IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM votes v
      WHERE v.user_id = qv.user_id
        AND v.battle_id = qv.quiz_id
  );

INSERT INTO votes (
    user_id, battle_id, pre_vote_option_id, post_vote_option_id,
    is_tts_listened, created_at, updated_at
)
SELECT
    pv.user_id,
    pv.poll_id,
    pv.option_id,
    pv.option_id,
    FALSE,
    pv.created_at,
    pv.updated_at
FROM poll_user_votes pv
WHERE to_regclass('public.poll_user_votes') IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM votes v
      WHERE v.user_id = pv.user_id
        AND v.battle_id = pv.poll_id
  );

-- 6) Recalculate legacy counters
WITH battle_counts AS (
    SELECT battle_id, COUNT(*)::BIGINT AS cnt
    FROM votes
    GROUP BY battle_id
)
UPDATE battles b
SET total_participants = bc.cnt
FROM battle_counts bc
WHERE b.id = bc.battle_id
  AND b.total_participants IS DISTINCT FROM bc.cnt;

UPDATE battles b
SET total_participants = 0
WHERE COALESCE(b.total_participants, 0) <> 0
  AND NOT EXISTS (SELECT 1 FROM votes v WHERE v.battle_id = b.id);

WITH option_counts AS (
    SELECT pre_vote_option_id AS option_id, COUNT(*)::BIGINT AS cnt
    FROM votes
    WHERE pre_vote_option_id IS NOT NULL
    GROUP BY pre_vote_option_id
)
UPDATE battle_options bo
SET vote_count = oc.cnt
FROM option_counts oc
WHERE bo.id = oc.option_id
  AND bo.vote_count IS DISTINCT FROM oc.cnt;

UPDATE battle_options bo
SET vote_count = 0
WHERE COALESCE(bo.vote_count, 0) <> 0
  AND NOT EXISTS (SELECT 1 FROM votes v WHERE v.pre_vote_option_id = bo.id);

-- 7) Ensure type for legacy battle rows
UPDATE battles b
SET type = 'BATTLE'
WHERE b.type IS NULL;

-- 8) Optional cleanup of split tables (drop only if they exist)
DROP TABLE IF EXISTS quiz_user_votes CASCADE;
DROP TABLE IF EXISTS poll_user_votes CASCADE;
DROP TABLE IF EXISTS quiz_options CASCADE;
DROP TABLE IF EXISTS poll_options CASCADE;
DROP TABLE IF EXISTS quizzes CASCADE;
DROP TABLE IF EXISTS poll_contents CASCADE;
