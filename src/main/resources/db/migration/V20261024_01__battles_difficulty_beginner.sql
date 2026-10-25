-- #571 수준이 비어 있는 배틀을 임시로 전부 초급으로 채운다 (어드민 상세에서 변경)
-- 멱등하게 작성되어 재실행해도 안전하다.
UPDATE battles SET difficulty = 'BEGINNER' WHERE difficulty IS NULL;
