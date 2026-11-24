-- #685 이용권별 사용 기간(일). NULL 이면 기본 6개월(등록 시각 + 6개월).
-- ddl-auto(update) 로도 컬럼은 생기지만 운영 반영 기록을 위해 둔다. 재실행해도 안전하다.

ALTER TABLE class_tickets ADD COLUMN IF NOT EXISTS usage_days INTEGER;
