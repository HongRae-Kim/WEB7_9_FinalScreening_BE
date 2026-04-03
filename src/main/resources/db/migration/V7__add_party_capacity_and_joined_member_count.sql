-- [1] party.capacity 추가
SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'party'
      AND COLUMN_NAME = 'capacity'
);

SET @query := IF(
    @col_exists = 0,
    'ALTER TABLE party ADD COLUMN capacity INT NOT NULL DEFAULT 1 AFTER leader_id',
    'SELECT "capacity already exists in party"'
);

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- [2] party.joined_member_count 추가
SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'party'
      AND COLUMN_NAME = 'joined_member_count'
);

SET @query := IF(
    @col_exists = 0,
    'ALTER TABLE party ADD COLUMN joined_member_count INT NOT NULL DEFAULT 1 AFTER capacity',
    'SELECT "joined_member_count already exists in party"'
);

PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- [3] capacity 백필
UPDATE party p
    JOIN post po ON po.post_id = p.post_id
    SET p.capacity = po.recruit_count
WHERE p.capacity IS NULL
   OR p.capacity = 1;

-- [4] joined_member_count 백필
UPDATE party p
    LEFT JOIN (
    SELECT pm.party_id, COUNT(*) AS joined_count
    FROM party_member pm
    WHERE pm.state = 'JOINED'
    GROUP BY pm.party_id
    ) pmc ON pmc.party_id = p.party_id
    SET p.joined_member_count = GREATEST(COALESCE(pmc.joined_count, 0), 1);

-- [5] 안전 보정
UPDATE party
SET capacity = 1
WHERE capacity IS NULL OR capacity < 1;

UPDATE party
SET joined_member_count = 1
WHERE joined_member_count IS NULL OR joined_member_count < 1;
