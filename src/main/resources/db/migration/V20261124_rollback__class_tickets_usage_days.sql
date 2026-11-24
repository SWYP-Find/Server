-- #685 롤백. 사용 기간을 정한 이용권은 다시 6개월 기준이 된다 (이미 등록된 이용권의 expires_at 은 그대로).

ALTER TABLE class_tickets DROP COLUMN IF EXISTS usage_days;
