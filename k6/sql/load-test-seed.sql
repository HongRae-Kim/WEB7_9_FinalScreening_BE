-- Load-test seed data for posts, parties, chats, and messages.
-- Safe to re-run: records are keyed off stable emails, nicknames, and memo strings.

SET @now = NOW(6);
SET @password_hash = '$2a$10$dVRkpPJ477B46S0VB2Z7Oeqiyt/dKIHi17.XU8xpRuvKVBqDIZJs2';

-- 1) Test users
INSERT INTO user (
    created_at, updated_at, deleted_at, is_active,
    email, password, nickname, comment, profile_image, verification_code, nickname_updated_at
)
SELECT @now, @now, NULL, b'1',
       'loadtest1@example.com', @password_hash, 'loadtest1',
       'load test user 1', NULL, 'VERIFIED', NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM user WHERE email = 'loadtest1@example.com');

INSERT INTO user (
    created_at, updated_at, deleted_at, is_active,
    email, password, nickname, comment, profile_image, verification_code, nickname_updated_at
)
SELECT @now, @now, NULL, b'1',
       'loadtest2@example.com', @password_hash, 'loadtest2',
       'load test user 2', NULL, 'VERIFIED', NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM user WHERE email = 'loadtest2@example.com');

INSERT INTO user (
    created_at, updated_at, deleted_at, is_active,
    email, password, nickname, comment, profile_image, verification_code, nickname_updated_at
)
SELECT @now, @now, NULL, b'1',
       'loadtest3@example.com', @password_hash, 'loadtest3',
       'load test user 3', NULL, 'VERIFIED', NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM user WHERE email = 'loadtest3@example.com');

INSERT INTO user (
    created_at, updated_at, deleted_at, is_active,
    email, password, nickname, comment, profile_image, verification_code, nickname_updated_at
)
SELECT @now, @now, NULL, b'1',
       'loadtest4@example.com', @password_hash, 'loadtest4',
       'load test user 4', NULL, 'VERIFIED', NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM user WHERE email = 'loadtest4@example.com');

INSERT INTO user (
    created_at, updated_at, deleted_at, is_active,
    email, password, nickname, comment, profile_image, verification_code, nickname_updated_at
)
SELECT @now, @now, NULL, b'1',
       'loadtest5@example.com', @password_hash, 'loadtest5',
       'load test user 5', NULL, 'VERIFIED', NULL
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM user WHERE email = 'loadtest5@example.com');

SET @user_main = (SELECT id FROM user WHERE email = 'loadtest1@example.com' LIMIT 1);
SET @user_2 = (SELECT id FROM user WHERE email = 'loadtest2@example.com' LIMIT 1);
SET @user_3 = (SELECT id FROM user WHERE email = 'loadtest3@example.com' LIMIT 1);
SET @user_4 = (SELECT id FROM user WHERE email = 'loadtest4@example.com' LIMIT 1);
SET @user_5 = (SELECT id FROM user WHERE email = 'loadtest5@example.com' LIMIT 1);

-- 2) Game accounts
INSERT INTO game_account (
    created_at, updated_at, deleted_at, is_active,
    game_nickname, game_tag, game_type, puuid, profile_icon_id, user_id
)
SELECT @now, @now, NULL, b'1',
       'MainTester', 'KR1', 'LOL', 'loadtest-puuid-main', 1234, @user_main
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM game_account WHERE user_id = @user_main);

INSERT INTO game_account (
    created_at, updated_at, deleted_at, is_active,
    game_nickname, game_tag, game_type, puuid, profile_icon_id, user_id
)
SELECT @now, @now, NULL, b'1',
       'LoadUser1', 'LT1', 'LOL', 'loadtest-puuid-1', 1235, @user_2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM game_account WHERE user_id = @user_2);

INSERT INTO game_account (
    created_at, updated_at, deleted_at, is_active,
    game_nickname, game_tag, game_type, puuid, profile_icon_id, user_id
)
SELECT @now, @now, NULL, b'1',
       'LoadUser2', 'LT2', 'LOL', 'loadtest-puuid-2', 1236, @user_3
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM game_account WHERE user_id = @user_3);

INSERT INTO game_account (
    created_at, updated_at, deleted_at, is_active,
    game_nickname, game_tag, game_type, puuid, profile_icon_id, user_id
)
SELECT @now, @now, NULL, b'1',
       'LoadUser3', 'LT3', 'LOL', 'loadtest-puuid-3', 1237, @user_4
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM game_account WHERE user_id = @user_4);

SET @ga_main = (SELECT game_account_id FROM game_account WHERE user_id = @user_main LIMIT 1);
SET @ga_2 = (SELECT game_account_id FROM game_account WHERE user_id = @user_2 LIMIT 1);
SET @ga_3 = (SELECT game_account_id FROM game_account WHERE user_id = @user_3 LIMIT 1);
SET @ga_4 = (SELECT game_account_id FROM game_account WHERE user_id = @user_4 LIMIT 1);

-- 3) Posts for list, party, and chat scenarios
INSERT INTO post (
    created_at, updated_at, deleted_at, is_active,
    game_mode, looking_positions, memo, mic, my_position, queue_type, recruit_count, status,
    game_account_id, user_id
)
SELECT @now, @now, NULL, b'1',
       'SUMMONERS_RIFT', JSON_ARRAY('JUNGLE', 'MID'), 'LOADTEST_POST_01',
       b'1', 'TOP', 'DUO', 3, 'RECRUIT', @ga_main, @user_main
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM post WHERE memo = 'LOADTEST_POST_01');

INSERT INTO post (
    created_at, updated_at, deleted_at, is_active,
    game_mode, looking_positions, memo, mic, my_position, queue_type, recruit_count, status,
    game_account_id, user_id
)
SELECT @now, @now, NULL, b'1',
       'SUMMONERS_RIFT', JSON_ARRAY('TOP', 'SUPPORT'), 'LOADTEST_POST_02',
       b'0', 'ADC', 'DUO', 2, 'RECRUIT', @ga_2, @user_2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM post WHERE memo = 'LOADTEST_POST_02');

INSERT INTO post (
    created_at, updated_at, deleted_at, is_active,
    game_mode, looking_positions, memo, mic, my_position, queue_type, recruit_count, status,
    game_account_id, user_id
)
SELECT @now, @now, NULL, b'1',
       'SUMMONERS_RIFT', JSON_ARRAY('ADC', 'SUPPORT'), 'LOADTEST_POST_03',
       b'1', 'MID', 'DUO', 2, 'RECRUIT', @ga_3, @user_3
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM post WHERE memo = 'LOADTEST_POST_03');

INSERT INTO post (
    created_at, updated_at, deleted_at, is_active,
    game_mode, looking_positions, memo, mic, my_position, queue_type, recruit_count, status,
    game_account_id, user_id
)
SELECT @now, @now, NULL, b'1',
       'SUMMONERS_RIFT', JSON_ARRAY('TOP', 'MID'), 'LOADTEST_POST_04',
       b'0', 'JUNGLE', 'FLEX', 5, 'RECRUIT', @ga_4, @user_4
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM post WHERE memo = 'LOADTEST_POST_04');

INSERT INTO post (
    created_at, updated_at, deleted_at, is_active,
    game_mode, looking_positions, memo, mic, my_position, queue_type, recruit_count, status,
    game_account_id, user_id
)
SELECT @now, @now, NULL, b'1',
       'HOWLING_ABYSS', JSON_ARRAY('ANY'), 'LOADTEST_POST_05',
       b'1', 'ANY', 'NORMAL', 5, 'RECRUIT', @ga_main, @user_main
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM post WHERE memo = 'LOADTEST_POST_05');

INSERT INTO post (
    created_at, updated_at, deleted_at, is_active,
    game_mode, looking_positions, memo, mic, my_position, queue_type, recruit_count, status,
    game_account_id, user_id
)
SELECT @now, @now, NULL, b'1',
       'ARENA', JSON_ARRAY('ANY'), 'LOADTEST_POST_06',
       b'0', 'ANY', 'NORMAL', 2, 'RECRUIT', @ga_2, @user_2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM post WHERE memo = 'LOADTEST_POST_06');

INSERT INTO post (
    created_at, updated_at, deleted_at, is_active,
    game_mode, looking_positions, memo, mic, my_position, queue_type, recruit_count, status,
    game_account_id, user_id
)
SELECT @now, @now, NULL, b'1',
       'SUMMONERS_RIFT', JSON_ARRAY('MID', 'JUNGLE'), 'LOADTEST_POST_07',
       b'1', 'SUPPORT', 'DUO', 2, 'RECRUIT', @ga_3, @user_3
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM post WHERE memo = 'LOADTEST_POST_07');

INSERT INTO post (
    created_at, updated_at, deleted_at, is_active,
    game_mode, looking_positions, memo, mic, my_position, queue_type, recruit_count, status,
    game_account_id, user_id
)
SELECT @now, @now, NULL, b'1',
       'SUMMONERS_RIFT', JSON_ARRAY('ADC', 'SUPPORT'), 'LOADTEST_POST_08',
       b'1', 'TOP', 'FLEX', 5, 'RECRUIT', @ga_4, @user_4
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM post WHERE memo = 'LOADTEST_POST_08');

INSERT INTO post (
    created_at, updated_at, deleted_at, is_active,
    game_mode, looking_positions, memo, mic, my_position, queue_type, recruit_count, status,
    game_account_id, user_id
)
SELECT @now, @now, NULL, b'1',
       'SUMMONERS_RIFT', JSON_ARRAY('JUNGLE', 'MID'), 'LOADTEST_POST_09',
       b'1', 'TOP', 'DUO', 3, 'RECRUIT', @ga_main, @user_main
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM post WHERE memo = 'LOADTEST_POST_09');

INSERT INTO post (
    created_at, updated_at, deleted_at, is_active,
    game_mode, looking_positions, memo, mic, my_position, queue_type, recruit_count, status,
    game_account_id, user_id
)
SELECT @now, @now, NULL, b'1',
       'SUMMONERS_RIFT', JSON_ARRAY('ADC', 'SUPPORT'), 'LOADTEST_POST_10',
       b'1', 'MID', 'DUO', 3, 'RECRUIT', @ga_main, @user_main
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM post WHERE memo = 'LOADTEST_POST_10');

INSERT INTO post (
    created_at, updated_at, deleted_at, is_active,
    game_mode, looking_positions, memo, mic, my_position, queue_type, recruit_count, status,
    game_account_id, user_id
)
SELECT @now, @now, NULL, b'1',
       'SUMMONERS_RIFT', JSON_ARRAY('TOP', 'MID'), 'LOADTEST_POST_11',
       b'0', 'JUNGLE', 'DUO', 3, 'RECRUIT', @ga_main, @user_main
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM post WHERE memo = 'LOADTEST_POST_11');

INSERT INTO post (
    created_at, updated_at, deleted_at, is_active,
    game_mode, looking_positions, memo, mic, my_position, queue_type, recruit_count, status,
    game_account_id, user_id
)
SELECT @now, @now, NULL, b'1',
       'SUMMONERS_RIFT', JSON_ARRAY('MID', 'SUPPORT'), 'LOADTEST_POST_12',
       b'1', 'ADC', 'DUO', 3, 'RECRUIT', @ga_main, @user_main
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM post WHERE memo = 'LOADTEST_POST_12');

SET @post_1 = (SELECT post_id FROM post WHERE memo = 'LOADTEST_POST_01' LIMIT 1);
SET @post_2 = (SELECT post_id FROM post WHERE memo = 'LOADTEST_POST_02' LIMIT 1);
SET @post_3 = (SELECT post_id FROM post WHERE memo = 'LOADTEST_POST_03' LIMIT 1);
SET @post_4 = (SELECT post_id FROM post WHERE memo = 'LOADTEST_POST_04' LIMIT 1);
SET @post_5 = (SELECT post_id FROM post WHERE memo = 'LOADTEST_POST_05' LIMIT 1);
SET @post_6 = (SELECT post_id FROM post WHERE memo = 'LOADTEST_POST_06' LIMIT 1);
SET @post_7 = (SELECT post_id FROM post WHERE memo = 'LOADTEST_POST_07' LIMIT 1);
SET @post_8 = (SELECT post_id FROM post WHERE memo = 'LOADTEST_POST_08' LIMIT 1);
SET @post_9 = (SELECT post_id FROM post WHERE memo = 'LOADTEST_POST_09' LIMIT 1);
SET @post_10 = (SELECT post_id FROM post WHERE memo = 'LOADTEST_POST_10' LIMIT 1);
SET @post_11 = (SELECT post_id FROM post WHERE memo = 'LOADTEST_POST_11' LIMIT 1);
SET @post_12 = (SELECT post_id FROM post WHERE memo = 'LOADTEST_POST_12' LIMIT 1);

-- 4) Parties and leader memberships (mirrors post creation flow)
INSERT INTO party (
    created_at, updated_at, deleted_at, is_active,
    closed_at, expires_at, leader_id, post_id, status, capacity, joined_member_count
)
SELECT @now, @now, NULL, b'1', NULL, NULL, @user_main, @post_1, 'RECRUIT', 2, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM party WHERE post_id = @post_1);

INSERT INTO party (
    created_at, updated_at, deleted_at, is_active,
    closed_at, expires_at, leader_id, post_id, status, capacity, joined_member_count
)
SELECT @now, @now, NULL, b'1', NULL, NULL, @user_2, @post_2, 'RECRUIT', 2, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM party WHERE post_id = @post_2);

INSERT INTO party (
    created_at, updated_at, deleted_at, is_active,
    closed_at, expires_at, leader_id, post_id, status, capacity, joined_member_count
)
SELECT @now, @now, NULL, b'1', NULL, NULL, @user_3, @post_3, 'RECRUIT', 2, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM party WHERE post_id = @post_3);

INSERT INTO party (
    created_at, updated_at, deleted_at, is_active,
    closed_at, expires_at, leader_id, post_id, status, capacity, joined_member_count
)
SELECT @now, @now, NULL, b'1', NULL, NULL, @user_4, @post_4, 'RECRUIT', 5, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM party WHERE post_id = @post_4);

INSERT INTO party (
    created_at, updated_at, deleted_at, is_active,
    closed_at, expires_at, leader_id, post_id, status, capacity, joined_member_count
)
SELECT @now, @now, NULL, b'1', NULL, NULL, @user_main, @post_9, 'RECRUIT', 3, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM party WHERE post_id = @post_9);

INSERT INTO party (
    created_at, updated_at, deleted_at, is_active,
    closed_at, expires_at, leader_id, post_id, status, capacity, joined_member_count
)
SELECT @now, @now, NULL, b'1', NULL, NULL, @user_main, @post_10, 'RECRUIT', 3, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM party WHERE post_id = @post_10);

INSERT INTO party (
    created_at, updated_at, deleted_at, is_active,
    closed_at, expires_at, leader_id, post_id, status, capacity, joined_member_count
)
SELECT @now, @now, NULL, b'1', NULL, NULL, @user_main, @post_11, 'RECRUIT', 3, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM party WHERE post_id = @post_11);

INSERT INTO party (
    created_at, updated_at, deleted_at, is_active,
    closed_at, expires_at, leader_id, post_id, status, capacity, joined_member_count
)
SELECT @now, @now, NULL, b'1', NULL, NULL, @user_main, @post_12, 'RECRUIT', 3, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM party WHERE post_id = @post_12);

SET @party_1 = (SELECT party_id FROM party WHERE post_id = @post_1 LIMIT 1);
SET @party_2 = (SELECT party_id FROM party WHERE post_id = @post_2 LIMIT 1);
SET @party_3 = (SELECT party_id FROM party WHERE post_id = @post_3 LIMIT 1);
SET @party_4 = (SELECT party_id FROM party WHERE post_id = @post_4 LIMIT 1);
SET @party_9 = (SELECT party_id FROM party WHERE post_id = @post_9 LIMIT 1);
SET @party_10 = (SELECT party_id FROM party WHERE post_id = @post_10 LIMIT 1);
SET @party_11 = (SELECT party_id FROM party WHERE post_id = @post_11 LIMIT 1);
SET @party_12 = (SELECT party_id FROM party WHERE post_id = @post_12 LIMIT 1);

INSERT INTO party_member (joined_at, left_at, role, state, party_id, user_id)
SELECT @now, NULL, 'LEADER', 'JOINED', @party_1, @user_main
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM party_member WHERE party_id = @party_1 AND user_id = @user_main);

INSERT INTO party_member (joined_at, left_at, role, state, party_id, user_id)
SELECT @now, NULL, 'LEADER', 'JOINED', @party_2, @user_2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM party_member WHERE party_id = @party_2 AND user_id = @user_2);

INSERT INTO party_member (joined_at, left_at, role, state, party_id, user_id)
SELECT @now, NULL, 'LEADER', 'JOINED', @party_3, @user_3
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM party_member WHERE party_id = @party_3 AND user_id = @user_3);

INSERT INTO party_member (joined_at, left_at, role, state, party_id, user_id)
SELECT @now, NULL, 'LEADER', 'JOINED', @party_4, @user_4
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM party_member WHERE party_id = @party_4 AND user_id = @user_4);

INSERT INTO party_member (joined_at, left_at, role, state, party_id, user_id)
SELECT @now, NULL, 'LEADER', 'JOINED', @party_9, @user_main
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM party_member WHERE party_id = @party_9 AND user_id = @user_main);

INSERT INTO party_member (joined_at, left_at, role, state, party_id, user_id)
SELECT @now, NULL, 'LEADER', 'JOINED', @party_10, @user_main
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM party_member WHERE party_id = @party_10 AND user_id = @user_main);

INSERT INTO party_member (joined_at, left_at, role, state, party_id, user_id)
SELECT @now, NULL, 'LEADER', 'JOINED', @party_11, @user_main
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM party_member WHERE party_id = @party_11 AND user_id = @user_main);

INSERT INTO party_member (joined_at, left_at, role, state, party_id, user_id)
SELECT @now, NULL, 'LEADER', 'JOINED', @party_12, @user_main
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM party_member WHERE party_id = @party_12 AND user_id = @user_main);

-- 4b) Dedicated write-bank posts/parties for realistic write sampling.
INSERT INTO post (
    created_at, updated_at, deleted_at, is_active,
    game_mode, looking_positions, memo, mic, my_position, queue_type, recruit_count, status,
    game_account_id, user_id
)
WITH RECURSIVE write_seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM write_seq WHERE n < 50
)
SELECT @now, @now, NULL, b'1',
       'SUMMONERS_RIFT', JSON_ARRAY('TOP', 'JUNGLE', 'MID', 'ADC'),
       CONCAT('LOADTEST_WRITE_', LPAD(write_seq.n, 3, '0')),
       b'1', 'SUPPORT', 'FLEX', 5, 'RECRUIT', @ga_main, @user_main
FROM write_seq
WHERE NOT EXISTS (
    SELECT 1
    FROM post
    WHERE memo = CONCAT('LOADTEST_WRITE_', LPAD(write_seq.n, 3, '0'))
);

INSERT INTO party (
    created_at, updated_at, deleted_at, is_active,
    closed_at, expires_at, leader_id, post_id, status, capacity, joined_member_count
)
WITH RECURSIVE write_seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM write_seq WHERE n < 50
)
SELECT @now, @now, NULL, b'1', NULL, NULL, @user_main, p.post_id, 'RECRUIT', 5, 1
FROM write_seq
JOIN post p ON p.memo = CONCAT('LOADTEST_WRITE_', LPAD(write_seq.n, 3, '0'))
WHERE NOT EXISTS (
    SELECT 1
    FROM party
    WHERE post_id = p.post_id
);

INSERT INTO party_member (joined_at, left_at, role, state, party_id, user_id)
WITH RECURSIVE write_seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM write_seq WHERE n < 50
)
SELECT @now, NULL, 'LEADER', 'JOINED', party.party_id, @user_main
FROM write_seq
JOIN post p ON p.memo = CONCAT('LOADTEST_WRITE_', LPAD(write_seq.n, 3, '0'))
JOIN party ON party.post_id = p.post_id
WHERE NOT EXISTS (
    SELECT 1
    FROM party_member
    WHERE party_id = party.party_id
      AND user_id = @user_main
);

-- Sync party capacity/joined_member_count with the actual seeded memberships.
UPDATE party
JOIN post ON post.post_id = party.post_id
SET party.capacity = post.recruit_count,
    party.joined_member_count = (
        SELECT COUNT(*)
        FROM party_member pm
        WHERE pm.party_id = party.party_id
          AND pm.state = 'JOINED'
    )
WHERE post.memo LIKE 'LOADTEST_%';

-- Additional members for the main user's party to support party-member reads.
INSERT INTO party_member (joined_at, left_at, role, state, party_id, user_id)
SELECT @now, NULL, 'MEMBER', 'JOINED', @party_1, @user_2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM party_member WHERE party_id = @party_1 AND user_id = @user_2);

INSERT INTO party_member (joined_at, left_at, role, state, party_id, user_id)
SELECT @now, NULL, 'MEMBER', 'JOINED', @party_1, @user_3
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM party_member WHERE party_id = @party_1 AND user_id = @user_3);

-- 5) Chat rooms based on the main user's post
INSERT INTO chat_room (
    created_at, updated_at, current_session_no,
    receiver_left, sender_left, session_started_at,
    post_id, receiver_id, sender_id
)
SELECT @now, @now, 1, b'0', b'0', @now, @post_1, @user_main, @user_2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM chat_room WHERE post_id = @post_1 AND sender_id = @user_2);

INSERT INTO chat_room (
    created_at, updated_at, current_session_no,
    receiver_left, sender_left, session_started_at,
    post_id, receiver_id, sender_id
)
SELECT @now, @now, 1, b'0', b'0', @now, @post_1, @user_main, @user_3
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM chat_room WHERE post_id = @post_1 AND sender_id = @user_3);

SET @room_1 = (SELECT chat_room_id FROM chat_room WHERE post_id = @post_1 AND sender_id = @user_2 LIMIT 1);
SET @room_2 = (SELECT chat_room_id FROM chat_room WHERE post_id = @post_1 AND sender_id = @user_3 LIMIT 1);

INSERT INTO chat_message_read (last_read_at, chat_room_id, last_read_message_id, user_id)
SELECT NULL, @room_1, NULL, @user_main
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM chat_message_read WHERE chat_room_id = @room_1 AND user_id = @user_main);

INSERT INTO chat_message_read (last_read_at, chat_room_id, last_read_message_id, user_id)
SELECT NULL, @room_1, NULL, @user_2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM chat_message_read WHERE chat_room_id = @room_1 AND user_id = @user_2);

INSERT INTO chat_message_read (last_read_at, chat_room_id, last_read_message_id, user_id)
SELECT NULL, @room_2, NULL, @user_main
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM chat_message_read WHERE chat_room_id = @room_2 AND user_id = @user_main);

INSERT INTO chat_message_read (last_read_at, chat_room_id, last_read_message_id, user_id)
SELECT NULL, @room_2, NULL, @user_3
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM chat_message_read WHERE chat_room_id = @room_2 AND user_id = @user_3);

-- 6) Messages for room and read scenarios
INSERT INTO chat_message (content, created_at, message_type, session_no, chat_room_id, sender_id)
SELECT 'hello from loadtest2', DATE_SUB(@now, INTERVAL 9 MINUTE), 'TEXT', 1, @room_1, @user_2
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM chat_message
    WHERE chat_room_id = @room_1 AND content = 'hello from loadtest2'
);

INSERT INTO chat_message (content, created_at, message_type, session_no, chat_room_id, sender_id)
SELECT 'reply from main user', DATE_SUB(@now, INTERVAL 8 MINUTE), 'TEXT', 1, @room_1, @user_main
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM chat_message
    WHERE chat_room_id = @room_1 AND content = 'reply from main user'
);

INSERT INTO chat_message (content, created_at, message_type, session_no, chat_room_id, sender_id)
SELECT 'party invite follow-up', DATE_SUB(@now, INTERVAL 7 MINUTE), 'TEXT', 1, @room_1, @user_2
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM chat_message
    WHERE chat_room_id = @room_1 AND content = 'party invite follow-up'
);

INSERT INTO chat_message (content, created_at, message_type, session_no, chat_room_id, sender_id)
SELECT 'hello from loadtest3', DATE_SUB(@now, INTERVAL 6 MINUTE), 'TEXT', 1, @room_2, @user_3
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM chat_message
    WHERE chat_room_id = @room_2 AND content = 'hello from loadtest3'
);

INSERT INTO chat_message (content, created_at, message_type, session_no, chat_room_id, sender_id)
SELECT 'main user reply to loadtest2', DATE_SUB(@now, INTERVAL 5 MINUTE), 'TEXT', 1, @room_2, @user_main
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM chat_message
    WHERE chat_room_id = @room_2 AND content = 'main user reply to loadtest2'
);

-- Sync one read state so unread count and read APIs both have data.
SET @room_1_last_msg = (
    SELECT chat_message_id
    FROM chat_message
    WHERE chat_room_id = @room_1
    ORDER BY chat_message_id DESC
    LIMIT 1
);

UPDATE chat_message_read
SET last_read_message_id = @room_1_last_msg,
    last_read_at = @now
WHERE chat_room_id = @room_1
  AND user_id = @user_main
  AND (last_read_message_id IS NULL OR last_read_message_id <> @room_1_last_msg);

-- Final verification snapshot
SELECT id, email, nickname FROM user ORDER BY id;
SELECT game_account_id, user_id, game_nickname FROM game_account ORDER BY game_account_id;
SELECT post_id, user_id, memo, status FROM post WHERE memo LIKE 'LOADTEST_POST_%' OR memo LIKE 'LOADTEST_WRITE_%' ORDER BY post_id;
SELECT party_id, post_id, leader_id, status FROM party ORDER BY party_id;
SELECT party_member_id, party_id, user_id, role, state FROM party_member ORDER BY party_member_id;
SELECT chat_room_id, post_id, receiver_id, sender_id FROM chat_room ORDER BY chat_room_id;
SELECT chat_message_id, chat_room_id, sender_id, content FROM chat_message ORDER BY chat_message_id;
