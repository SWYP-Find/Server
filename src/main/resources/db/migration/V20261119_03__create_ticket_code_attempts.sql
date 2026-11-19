-- #653 이용권 코드 입력 실패 횟수 (계정별 1행).
-- 코드가 PKE+16진수 6자리로 짧아져, 1시간 안에 없는 코드를 5번 넣으면 1시간 동안 코드 확인·등록을 막는다.
-- 서버 배포 전에 먼저 실행해야 한다 (없으면 코드 확인·등록이 500).
-- 멱등하게 작성되어 재실행해도 안전하다.

CREATE TABLE IF NOT EXISTS ticket_code_attempts (
    user_id BIGINT PRIMARY KEY,
    fail_count INT NOT NULL DEFAULT 0,
    window_started_at TIMESTAMP,
    locked_until TIMESTAMP,
    updated_at TIMESTAMP NOT NULL
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_ticket_code_attempts_user') THEN
        ALTER TABLE ticket_code_attempts ADD CONSTRAINT fk_ticket_code_attempts_user
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
END $$;
