-- 유저당 하나만 생성 가능하게 막고 있는 잘못된 제약 조건 삭제
ALTER TABLE review_request DROP INDEX uk_review_request_post_user;