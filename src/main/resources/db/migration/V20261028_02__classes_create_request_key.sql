-- #576 클래스 생성 중복 요청 방지: 클라이언트가 보낸 요청 키와 본문 해시를 저장한다.
-- 같은 운영자 안에서 키가 유일하다. 키 없이 만든 클래스(NULL)는 UNIQUE 에서 서로 다른 값으로 취급된다.
-- 멱등하게 작성되어 재실행해도 안전하다.

ALTER TABLE classes ADD COLUMN IF NOT EXISTS create_request_key VARCHAR(64);
ALTER TABLE classes ADD COLUMN IF NOT EXISTS create_request_hash VARCHAR(64);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_classes_owner_request_key') THEN
        ALTER TABLE classes ADD CONSTRAINT uk_classes_owner_request_key UNIQUE (owner_id, create_request_key);
    END IF;
END $$;
