-- #565 이용 종료 후 30일 지난 클래스 데이터 파기 (PRD §18)
-- 멱등하게 작성되어 재실행해도 안전하다.
ALTER TABLE classes ADD COLUMN IF NOT EXISTS purged_at TIMESTAMP;
