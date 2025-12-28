-- User 테이블 닉네임 업데이트 일시 컬럼 추가
ALTER TABLE user ADD COLUMN nickname_updated_at DATETIME(6);
