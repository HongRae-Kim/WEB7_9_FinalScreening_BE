-- 1. Review 테이블 수정 (Post 참조 -> Party 참조)
-- 1-1. 기존 Foreign Key 제거 (post_id 참조)
ALTER TABLE review DROP FOREIGN KEY FKrl7b0my7pmicpl5l591p7qdu7;

-- 1-2. 기존 Unique Key 제거 (reviewer + reviewee + post 조합)
ALTER TABLE review DROP INDEX uk_review_reviewer_reviewee_post;

-- 1-3. post_id 컬럼 삭제 및 party_id 추가
ALTER TABLE review DROP COLUMN post_id;
ALTER TABLE review ADD COLUMN party_id BIGINT NOT NULL;

-- 1-4. 새로운 제약 조건 추가
ALTER TABLE review ADD CONSTRAINT fk_review_party FOREIGN KEY (party_id) REFERENCES party (party_id);
ALTER TABLE review ADD CONSTRAINT uk_review_reviewer_reviewee_party UNIQUE (reviewer_id, reviewee_id, party_id);


-- 2. ReviewRequest 테이블 수정 (Post 참조 -> Party 참조)
-- 2-1. 기존 Foreign Key 제거 (post_id 참조)
ALTER TABLE review_request DROP FOREIGN KEY FK3ak0tik0pid86c0tfrixbbga4;

-- 2-2. 기존 Unique Key 제거 (post + user 조합)
ALTER TABLE review_request DROP INDEX uk_review_request_post_user;

-- 2-3. post_id 컬럼 삭제 및 party_id 추가
ALTER TABLE `review_request` DROP COLUMN post_id;
ALTER TABLE `review_request` ADD COLUMN party_id BIGINT NOT NULL;

-- 2-4. 새로운 제약 조건 추가
ALTER TABLE review_request ADD CONSTRAINT fk_review_request_party FOREIGN KEY (party_id) REFERENCES party (party_id);
ALTER TABLE review_request ADD CONSTRAINT uk_review_request_party_user UNIQUE (party_id, request_user_id);