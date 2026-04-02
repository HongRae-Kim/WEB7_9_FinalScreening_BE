-- Reset realistic write-bank targets so party_add_members can exercise the successful insert path again.
-- Also resets the older LOADTEST_POST_09~12 compatibility helpers.

SET @user_2 = (SELECT id FROM user WHERE email = 'loadtest2@example.com' LIMIT 1);
SET @user_3 = (SELECT id FROM user WHERE email = 'loadtest3@example.com' LIMIT 1);
SET @user_4 = (SELECT id FROM user WHERE email = 'loadtest4@example.com' LIMIT 1);
SET @user_5 = (SELECT id FROM user WHERE email = 'loadtest5@example.com' LIMIT 1);

DELETE party_member
FROM party_member
JOIN party ON party.party_id = party_member.party_id
JOIN post ON post.post_id = party.post_id
WHERE party_member.user_id IN (@user_2, @user_3, @user_4, @user_5)
  AND (
      post.memo LIKE 'LOADTEST_WRITE_%'
      OR post.memo IN ('LOADTEST_POST_09', 'LOADTEST_POST_10', 'LOADTEST_POST_11', 'LOADTEST_POST_12')
  );

UPDATE party
JOIN post ON post.post_id = party.post_id
SET party.status = 'RECRUIT',
    party.expires_at = NULL,
    party.closed_at = NULL,
    party.capacity = post.recruit_count,
    party.joined_member_count = 1
WHERE post.memo LIKE 'LOADTEST_WRITE_%'
   OR post.memo IN ('LOADTEST_POST_09', 'LOADTEST_POST_10', 'LOADTEST_POST_11', 'LOADTEST_POST_12');

UPDATE post
SET status = 'RECRUIT'
WHERE memo LIKE 'LOADTEST_WRITE_%'
   OR memo IN ('LOADTEST_POST_09', 'LOADTEST_POST_10', 'LOADTEST_POST_11', 'LOADTEST_POST_12');
