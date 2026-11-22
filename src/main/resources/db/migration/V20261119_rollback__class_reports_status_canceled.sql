-- #651 V20261119_02 되돌리기. CANCELED 리포트를 FAILED 로 바꾼 뒤, 제약이 있는 환경이면 이전 값 목록으로 다시 만든다.
-- FAILED 로 바뀐 리포트는 운영자 재시도 대상이 되므로, 되돌린 뒤에는 삭제된 클래스의 리포트를 재시도하지 않도록 주의한다.

UPDATE class_reports SET status = 'FAILED' WHERE status = 'CANCELED';

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'class_reports_status_check' AND conrelid = 'class_reports'::regclass) THEN
        ALTER TABLE class_reports DROP CONSTRAINT class_reports_status_check;
        ALTER TABLE class_reports ADD CONSTRAINT class_reports_status_check CHECK (status IN (
            'PENDING', 'GENERATING', 'SUCCEEDED', 'LIMITED', 'FAILED'));
    END IF;
END $$;
