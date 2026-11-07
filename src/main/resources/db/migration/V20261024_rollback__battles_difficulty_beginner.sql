-- Rollback for V20261024_01__battles_difficulty_beginner.sql
-- 어떤 행을 채웠는지 따로 기록하지 않으므로, 어드민이 초급으로 직접 고른 배틀도 함께 비워진다.
UPDATE battles SET difficulty = NULL WHERE difficulty = 'BEGINNER' AND creator_type <> 'USER';
