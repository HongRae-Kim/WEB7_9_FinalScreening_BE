-- 유저당 하나만 생성 가능하게 막고 있는 잘못된 제약 조건 삭제
-- 1. FK 삭제
ALTER TABLE review_request DROP FOREIGN KEY FKi6fybm4ude799rrg8wj2i3abl;

-- 2. 잘못된 단독 유니크 인덱스 삭제
ALTER TABLE review_request DROP INDEX uk_review_request_post_user;

-- 3. FK 재설정 (기존에 가졌던 제약조건 옵션을 반드시 확인 후 동일하게 부여)
ALTER TABLE review_request ADD CONSTRAINT FKi6fybm4ude799rrg8wj2i3abl
    FOREIGN KEY (request_user_id) REFERENCES user (id);