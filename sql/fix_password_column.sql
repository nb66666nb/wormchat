-- ============================================================
-- 修复注册失败：user_info.password 列长度扩容
--
-- 背景：
--   项目历史密码方案为 MD5（32 位十六进制密文），表结构中 password
--   列可能定义为 varchar(32)。现注册改用 BCrypt（固定 60 字符密文，
--   形如 $2a$10$xxxx...），超出列长度导致 INSERT 报
--   "Data too long for column 'password'"，注册必定失败。
--
-- 用法：在 MySQL 中执行
--   mysql -uroot -p easymetting < fix_password_column.sql
-- ============================================================

-- 1. 先查看当前列定义（确认是否为 varchar(32)）
SHOW COLUMNS FROM `user_info` LIKE 'password';

-- 2. 扩容密码列（兼容 BCrypt 60 字符密文，留余量到 100）
ALTER TABLE `user_info`
    MODIFY COLUMN `password` varchar(100) NOT NULL COMMENT '密码（BCrypt密文）';

-- 3. 验证修改结果
SHOW COLUMNS FROM `user_info` LIKE 'password';
