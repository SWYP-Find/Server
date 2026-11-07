-- Rollback for V20261028_01__create_class_mid_votes.sql
-- 클래스 중간투표 기록이 모두 삭제된다. 애플리케이션 코드를 #577 이전으로 되돌린 뒤 실행할 것.
DROP TABLE IF EXISTS class_mid_votes;
