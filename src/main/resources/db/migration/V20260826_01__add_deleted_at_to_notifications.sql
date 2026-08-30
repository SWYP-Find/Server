-- 관리자 공지사항 soft delete를 위한 deleted_at 컬럼 추가
ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
