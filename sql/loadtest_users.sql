-- ============================================================
-- 压测账号批量插入脚本
-- 生成 200 个账号：loadtest001@qq.com ~ loadtest200@qq.com
-- 密码统一为 test123456 (MD5: 47ec2dd791e31e2ef2076caf64ed9b3d)
-- ============================================================
SET NAMES utf8mb4;

-- 使用数字表（0-9 的笛卡尔积）生成 1~200 的序号
INSERT INTO `user_info`
    (`user_id`, `nick_name`, `password`, `email`, `sex`, `status`, `meeting_no`, `create_time`)
SELECT
    CONCAT('LOAD', LPAD(n.num, 3, '0'))                 AS user_id,
    CONCAT('压测用户', n.num)                            AS nick_name,
    '47ec2dd791e31e2ef2076caf64ed9b3d'                  AS password,
    CONCAT('loadtest', LPAD(n.num, 3, '0'), '@qq.com')  AS email,
    2                                                    AS sex,
    1                                                    AS status,
    CONCAT('GLD', LPAD(n.num, 3, '0'))                  AS meeting_no,
    NOW()                                                AS create_time
FROM (
    SELECT a.a + b.b*10 + c.c*100 AS num
    FROM
        (SELECT 0 AS a UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
         UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a,
        (SELECT 0 AS b UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
         UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b,
        (SELECT 0 AS c UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
         UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) c
) n
WHERE n.num BETWEEN 1 AND 200
ON DUPLICATE KEY UPDATE `email` = VALUES(`email`);
