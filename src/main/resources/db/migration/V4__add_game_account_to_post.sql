-- 1. 컬럼 추가 (기존 데이터가 있을 수 있으므로 우선 NULL 허용으로 생성)
SET @col_exists := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'post' AND COLUMN_NAME = 'game_account_id');
SET @query := IF(@col_exists = 0, 'ALTER TABLE post ADD COLUMN game_account_id BIGINT', 'SELECT "game_account_id already exists"');
PREPARE stmt FROM @query; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 특정 ID로 업데이트 로직
UPDATE post SET game_account_id = 1 WHERE user_id = 2;
UPDATE post SET game_account_id = 2 WHERE user_id = 4;
UPDATE post SET game_account_id = 5 WHERE user_id = 6;
UPDATE post SET game_account_id = 6 WHERE user_id = 7;
UPDATE post SET game_account_id = 8 WHERE user_id = 9;

-- 3. NOT NULL 제약 조건으로 변경
ALTER TABLE post MODIFY COLUMN game_account_id BIGINT NOT NULL;

-- 4. 외래 키 제약 조건 추가
SET @constraint_exists := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'post'
                             AND CONSTRAINT_NAME = 'fk_post_game_account');
SET @query := IF(@constraint_exists = 0,
                 'ALTER TABLE post ADD CONSTRAINT fk_post_game_account FOREIGN KEY (game_account_id) REFERENCES game_account (game_account_id)',
                 'SELECT "FK already exists"');
PREPARE stmt FROM @query; EXECUTE stmt; DEALLOCATE PREPARE stmt;