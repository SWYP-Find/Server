-- #620 기획 문의로 남겨 둔 배틀의 기획 답변 반영 (#610, #618 이후).
-- 제목은 V20261107_01 과 같이 공백·문장부호를 빼고 비교한다. 사용자가 만든 배틀(USER)과 삭제된 배틀은 제외한다.
-- "행복은 순간의 감정일까, 남겨진 기억일까?" 는 주제가 모호해 내리기로 해 어드민에서 처리한다 (이 SQL 대상 아님).
-- 멱등하게 작성되어 재실행해도 안전하다. 한 트랜잭션으로 실행한다.

BEGIN;

-- 잘못한 사람, 엄벌할까 사랑으로 감쌀까? → 초급
UPDATE battles
SET difficulty = 'BEGINNER'
WHERE regexp_replace(lower(title), '[[:space:][:punct:]‘’“”·…「」『』]', '', 'g') = '잘못한사람엄벌할까사랑으로감쌀까'
  AND deleted_at IS NULL AND creator_type <> 'USER'
  AND difficulty IS DISTINCT FROM 'BEGINNER';

-- 살기 위해 반역의 왕위, 받아들일까? → 기존 콘텐츠 파일과 같이 역사, 중급
-- 같은 제목이 기획 표에 사회 초급·역사 중급으로 있어 카테고리가 비어 있는 배틀만 역사로 채운다
UPDATE battles
SET category = COALESCE(category, 'HISTORY'),
    difficulty = 'INTERMEDIATE'
WHERE regexp_replace(lower(title), '[[:space:][:punct:]‘’“”·…「」『』]', '', 'g') = '살기위해반역의왕위받아들일까'
  AND deleted_at IS NULL AND creator_type <> 'USER'
  AND (category IS NULL OR category = 'HISTORY')
  AND (category IS NULL OR difficulty IS DISTINCT FROM 'INTERMEDIATE');

COMMIT;
