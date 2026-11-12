-- V20261112_01 되돌리기. 저장된 분류 결과가 함께 지워진다.

ALTER TABLE class_debate_messages DROP COLUMN IF EXISTS response_type;
ALTER TABLE class_debate_messages DROP COLUMN IF EXISTS valid_argument;
ALTER TABLE class_debate_messages DROP COLUMN IF EXISTS valid_for_analysis;
ALTER TABLE class_debate_messages DROP COLUMN IF EXISTS input_types;
