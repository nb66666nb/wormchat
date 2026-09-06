-- 群聊信息表
CREATE TABLE IF NOT EXISTS `group_info` (
    `group_id`          VARCHAR(64) NOT NULL COMMENT '群ID（主键）',
    `group_name`        VARCHAR(100) NOT NULL DEFAULT '' COMMENT '群名称',
    `group_avatar`      VARCHAR(255) NOT NULL DEFAULT '' COMMENT '群头像路径',
    `owner_user_id`     VARCHAR(64) NOT NULL DEFAULT '' COMMENT '群主用户ID',
    `invite_permission` TINYINT NOT NULL DEFAULT 0 COMMENT '邀请权限：0-所有人可邀请，1-仅群主/管理员可邀请',
    `announcement`      VARCHAR(500) NOT NULL DEFAULT '' COMMENT '群公告',
    `status`            TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-正常，1-已解散',
    `create_time`       BIGINT NOT NULL DEFAULT 0 COMMENT '创建时间戳(ms)',
    `update_time`       BIGINT NOT NULL DEFAULT 0 COMMENT '更新时间戳(ms)',
    PRIMARY KEY (`group_id`),
    INDEX `idx_group_info_owner` (`owner_user_id`),
    INDEX `idx_group_info_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群聊信息表';

-- 群成员表
CREATE TABLE IF NOT EXISTS `group_member` (
    `id`                BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `group_id`          VARCHAR(64) NOT NULL COMMENT '群ID',
    `user_id`           VARCHAR(64) NOT NULL COMMENT '用户ID',
    `role`              VARCHAR(10) NOT NULL DEFAULT 'MEMBER' COMMENT '角色：OWNER-群主，ADMIN-管理员，MEMBER-普通成员',
    `nickname_in_group` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '群昵称',
    `join_time`         BIGINT NOT NULL DEFAULT 0 COMMENT '加入时间戳(ms)',
    `status`            TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-正常，1-主动退出，2-被踢出',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_group_user` (`group_id`, `user_id`),
    INDEX `idx_group_member_group` (`group_id`),
    INDEX `idx_group_member_user` (`user_id`),
    INDEX `idx_group_member_status` (`group_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群成员表';
