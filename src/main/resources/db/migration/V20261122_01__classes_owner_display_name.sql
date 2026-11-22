-- #669 미참여 운영자(classes.owner_participates = false)의 클래스 표시 이름.
-- ddl-auto(update) 로도 컬럼은 생기지만 운영 반영 기록을 위해 둔다. 재실행해도 안전하다.
-- 기존 클래스는 NULL 로 두며, NULL 이면 계정 닉네임을 보여준다.

ALTER TABLE classes ADD COLUMN IF NOT EXISTS owner_display_name VARCHAR(20);
