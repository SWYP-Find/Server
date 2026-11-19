-- #651 class_reports.status 에 CANCELED(클래스 삭제·멤버 나가기로 생성하지 않은 리포트) 추가.
-- 마이그레이션 SQL 로 만든 class_reports 에는 status 제약이 없다. Hibernate 가 테이블을 만든 환경에는 class_reports_status_check 가 있을 수 있어,
-- 있으면 새 값을 포함해 다시 만든다. 없으면 아무것도 하지 않는다. 멱등하게 작성되어 재실행해도 안전하다.

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'class_reports_status_check' AND conrelid = 'class_reports'::regclass) THEN
        ALTER TABLE class_reports DROP CONSTRAINT class_reports_status_check;
        ALTER TABLE class_reports ADD CONSTRAINT class_reports_status_check CHECK (status IN (
            'PENDING', 'GENERATING', 'SUCCEEDED', 'LIMITED', 'FAILED', 'CANCELED'));
    END IF;
END $$;
