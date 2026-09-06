-- ============================================================
-- 压测账号补充脚本：插入 loadtest201 ~ loadtest1000（共 800 个）
-- 密码统一 test123456 (MD5: 47ec2dd791e31e2ef2076caf64ed9b3d)
-- 与 loadtest_users.sql（1~200）配合，最终共 1000 个账号
-- ============================================================
SET NAMES utf8mb4;

INSERT INTO `user_info`
    (`user_id`, `nick_name`, `password`, `email`, `sex`, `status`, `meeting_no`, `create_time`)
SELECT
    CONCAT('LOAD', n.num)                                    AS user_id,
    CONCAT('压测用户', n.num)                                  AS nick_name,
    '47ec2dd791e31e2ef2076caf64ed9b3d'                       AS password,
    CONCAT('loadtest', n.num, '@qq.com')                     AS email,
    2                                                         AS sex,
    1                                                         AS status,
    CONCAT('GLD', n.num)                                     AS meeting_no,
    NOW()                                                     AS create_time
FROM (
    SELECT a.a + b.b*10 + c.c*100 + d.d*1000 AS num
    FROM
        (SELECT 0 AS a UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
         UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a,
        (SELECT 0 AS b UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
         UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b,
        (SELECT 0 AS c UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
         UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) c,
        (SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
         UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d
) n
WHERE n.num BETWEEN 201 AND 1000
ON DUPLICATE KEY UPDATE `email` = VALUES(`email`);
