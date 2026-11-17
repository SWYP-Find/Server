-- #640 ai_jobs.type 에 DEBATE_INPUT_CLASSIFY(반박 입력 분류) 추가.
-- 마이그레이션 SQL 로 만든 ai_jobs 에는 type 제약이 없다. Hibernate 가 테이블을 만든 환경에는 ai_jobs_type_check 가 있을 수 있어,
-- 있으면 새 값을 포함해 다시 만든다. 없으면 아무것도 하지 않는다. 멱등하게 작성되어 재실행해도 안전하다.

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ai_jobs_type_check' AND conrelid = 'ai_jobs'::regclass) THEN
        ALTER TABLE ai_jobs DROP CONSTRAINT ai_jobs_type_check;
        ALTER TABLE ai_jobs ADD CONSTRAINT ai_jobs_type_check CHECK (type IN (
            'TOPIC_CANDIDATES', 'TOPIC_VALIDATION', 'DEBATE_ARGUMENT', 'DEBATE_INPUT_CLASSIFY', 'MEMBER_REPORT', 'CLASS_INSIGHT'));
    END IF;
END $$;
