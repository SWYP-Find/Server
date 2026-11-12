-- #632 커스텀 배틀 반박 입력 분류 저장 (엣지 케이스 PRD 4.1).
-- input_types: 쉼표로 이은 입력 유형 (NORMAL, SHORT_VALID, INSUFFICIENT, OFF_TOPIC, PROFANITY, AGGRESSIVE, MEANINGLESS, INJECTION)
-- valid_for_analysis: 리포트 분석에 쓸 수 있는지, valid_argument: 확인된 논거 한 문장, response_type: NORMAL / CLARIFY / RETURN_TO_TOPIC
-- 기존 행은 분류하지 않았으므로 모두 NULL 로 둔다. 멱등하게 작성되어 재실행해도 안전하다.

ALTER TABLE class_debate_messages ADD COLUMN IF NOT EXISTS input_types VARCHAR(120);
ALTER TABLE class_debate_messages ADD COLUMN IF NOT EXISTS valid_for_analysis BOOLEAN;
ALTER TABLE class_debate_messages ADD COLUMN IF NOT EXISTS valid_argument TEXT;
ALTER TABLE class_debate_messages ADD COLUMN IF NOT EXISTS response_type VARCHAR(20);
