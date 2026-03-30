-- Reset fresh party-write targets so party_add_members hits the successful insert path again.
-- Intended for the loadtest1 leader with target users loadtest2/loadtest3.

SET @user_2 = (SELECT id FROM user WHERE email = 'loadtest2@example.com' LIMIT 1);
SET @user_3 = (SELECT id FROM user WHERE email = 'loadtest3@example.com' LIMIT 1);

SET @post_9 = (SELECT post_id FROM post WHERE memo = 'LOADTEST_POST_09' LIMIT 1);
SET @post_10 = (SELECT post_id FROM post WHERE memo = 'LOADTEST_POST_10' LIMIT 1);
SET @post_11 = (SELECT post_id FROM post WHERE memo = 'LOADTEST_POST_11' LIMIT 1);
SET @post_12 = (SELECT post_id FROM post WHERE memo = 'LOADTEST_POST_12' LIMIT 1);

SET @party_9 = (SELECT party_id FROM party WHERE post_id = @post_9 LIMIT 1);
SET @party_10 = (SELECT party_id FROM party WHERE post_id = @post_10 LIMIT 1);
SET @party_11 = (SELECT party_id FROM party WHERE post_id = @post_11 LIMIT 1);
SET @party_12 = (SELECT party_id FROM party WHERE post_id = @post_12 LIMIT 1);

DELETE FROM party_member
WHERE party_id IN (@party_9, @party_10, @party_11, @party_12)
  AND user_id IN (@user_2, @user_3);

UPDATE party
SET status = 'RECRUIT',
    expires_at = NULL,
    closed_at = NULL
WHERE party_id IN (@party_9, @party_10, @party_11, @party_12);

UPDATE post
SET status = 'RECRUIT'
WHERE post_id IN (@post_9, @post_10, @post_11, @post_12);
