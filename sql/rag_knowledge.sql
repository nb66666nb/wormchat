-- RAG 知识库表（MySQL FULLTEXT + ngram 中文分词）
-- 执行方式: 直接在 MySQL 客户端或 Navicat 中运行

CREATE TABLE IF NOT EXISTS rag_knowledge (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    bot_id      VARCHAR(32)     DEFAULT NULL  COMMENT '关联机器人ID',
    session_id  VARCHAR(32)     DEFAULT NULL  COMMENT '关联会话ID',
    content     LONGTEXT        NOT NULL COMMENT '知识内容',
    source      VARCHAR(500)    DEFAULT NULL  COMMENT '来源标识（文件名/URL等）',
    chunk_index INT             DEFAULT 0    COMMENT '分块序号',
    create_time DATETIME        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_bot_id (bot_id),
    FULLTEXT INDEX ft_content (content) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG知识库';

-- 检查 ngram 是否启用（MySQL 5.7.6+ 内置）
-- SHOW VARIABLES LIKE 'ngram_token_size';
-- ngram_token_size 默认为 2（最小分词长度=2个字符），适合中文

-- 插入示例数据（可选）
-- INSERT INTO rag_knowledge (content, source) VALUES
-- ('EasyMeeting 是一个基于 WebRTC 的视频会议系统，支持最多 100 人同时参会', '产品手册'),
-- ('会议创建步骤：点击新建会议 → 输入会议主题 → 选择开始时间 → 邀请成员', '帮助文档'),
-- ('机器人支持三种类型：功能型(FUNCTIONAL)、陪伴型(COMPANION)、混合型(HYBRID)', '技术文档');
