-- Rollback for V20261019_01__create_class_schema.sql
-- 클래스 데이터가 모두 삭제된다. 애플리케이션 코드를 #528 이전으로 되돌린 뒤 실행할 것.

-- 1) 기존 테이블의 클래스 컬럼 / 인덱스 제거 (FK 는 컬럼 drop 시 함께 제거됨)
DROP INDEX IF EXISTS ux_votes_user_battle_class_nn;
DROP INDEX IF EXISTS ux_user_battles_user_battle_class_nn;
DROP INDEX IF EXISTS idx_votes_class;
DROP INDEX IF EXISTS idx_perspectives_class;
DROP INDEX IF EXISTS idx_perspective_comments_class;
DROP INDEX IF EXISTS idx_user_battles_class;
DROP INDEX IF EXISTS idx_battles_class_recommend;

ALTER TABLE votes DROP CONSTRAINT IF EXISTS uk_votes_user_battle_class;
ALTER TABLE user_battles DROP CONSTRAINT IF EXISTS uk_user_battles_user_battle_class;

ALTER TABLE votes DROP COLUMN IF EXISTS class_id;
ALTER TABLE votes DROP COLUMN IF EXISTS mid_vote_option_id;
ALTER TABLE perspectives DROP COLUMN IF EXISTS class_id;
ALTER TABLE perspective_comments DROP COLUMN IF EXISTS class_id;
ALTER TABLE user_battles DROP COLUMN IF EXISTS class_id;

ALTER TABLE battles DROP COLUMN IF EXISTS difficulty;
ALTER TABLE battles DROP COLUMN IF EXISTS category;
ALTER TABLE battles DROP COLUMN IF EXISTS class_recommendable;
ALTER TABLE battles DROP COLUMN IF EXISTS mid_vote_enabled;

-- 2) 기존 UK(user_id, battle_id) 복구. 중복 행이 있으면 실패하므로 먼저 정리 필요
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_user_battle') THEN
        ALTER TABLE user_battles ADD CONSTRAINT uk_user_battle UNIQUE (user_id, battle_id);
    END IF;
END $$;

-- 3) 신규 테이블 제거 (의존 순서)
DROP TABLE IF EXISTS class_reports CASCADE;
DROP TABLE IF EXISTS ai_jobs CASCADE;
DROP TABLE IF EXISTS class_members CASCADE;
DROP TABLE IF EXISTS classes CASCADE;
DROP TABLE IF EXISTS ticket_delivery_attempts CASCADE;
DROP TABLE IF EXISTS class_tickets CASCADE;
DROP TABLE IF EXISTS funding_orders CASCADE;
