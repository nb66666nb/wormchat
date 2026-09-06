package com.meetchat.ai.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * MySQL FULLTEXT 文档存储实现
 *
 * 建表SQL（需手动执行一次）：
 * <pre>
 * CREATE TABLE IF NOT EXISTS rag_knowledge (
 *   id          BIGINT AUTO_INCREMENT PRIMARY KEY,
 *   content     LONGTEXT        NOT NULL COMMENT '知识内容',
 *   source      VARCHAR(500)    DEFAULT NULL  COMMENT '来源标识（文件名/URL等）',
 *   create_time DATETIME        DEFAULT CURRENT_TIMESTAMP,
 *   FULLTEXT INDEX ft_content (content) WITH PARSER ngram
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
 * </pre>
 *
 * ngram parser 启用中文分词。MySQL 5.7.6+ 内置。
 * 低版本 MySQL 可改用: FULLTEXT INDEX ft_content (content)
 */
@Component("mysqlDocumentStore")
public class MysqlDocumentStore implements DocumentStore {

    private static final Logger logger = LoggerFactory.getLogger(MysqlDocumentStore.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 全文检索返回的最大条数 */
    private static final int MAX_SEARCH_RESULTS = 10;

    @Override
    public boolean isAvailable() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            logger.warn("MySQL DocumentStore 不可用", e);
            return false;
        }
    }

    @Override
    public void indexDocuments(List<RagDocument> documents) {
        if (documents == null || documents.isEmpty()) return;

        String sql = "INSERT INTO rag_knowledge (content, source, bot_id, chunk_index) VALUES (?, ?, ?, ?)";

        for (RagDocument doc : documents) {
            try {
                jdbcTemplate.update(sql,
                        doc.getContent(),
                        doc.getSource() != null ? doc.getSource() : "manual",
                        doc.getBotId(),
                        doc.getChunkIndex() != null ? doc.getChunkIndex() : 0);
            } catch (Exception e) {
                logger.error("索引文档失败: source={}", doc.getSource(), e);
            }
        }
        logger.info("索引 {} 条文档完成", documents.size());
    }

    @Override
    public List<RagDocument> search(String query, int topK) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        int limit = Math.min(topK, MAX_SEARCH_RESULTS);

        try {
            // 使用 MySQL 全文检索: MATCH ... AGAINST ... IN BOOLEAN MODE
            // 将用户查询拆词后拼接为 +word* 格式以支持前缀匹配
            String booleanQuery = buildBooleanQuery(query);
            String sql = "SELECT id, content, source, " +
                    "MATCH(content) AGAINST(? IN BOOLEAN MODE) AS score " +
                    "FROM rag_knowledge " +
                    "WHERE MATCH(content) AGAINST(? IN BOOLEAN MODE) " +
                    "ORDER BY score DESC LIMIT ?";

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    sql, booleanQuery, booleanQuery, limit);

            List<RagDocument> results = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                RagDocument doc = new RagDocument();
                doc.setId(String.valueOf(row.get("id")));
                doc.setContent((String) row.get("content"));
                doc.setSource((String) row.get("source"));
                Object scoreObj = row.get("score");
                doc.setScore(scoreObj != null ? ((Number) scoreObj).doubleValue() : 0.0);
                results.add(doc);
            }

            // 如果 FULLTEXT 无结果，回退到 LIKE 模糊匹配
            if (results.isEmpty()) {
                results = fallbackLikeSearch(query, limit);
            }

            logger.info("RAG检索: query={}, results={}", query, results.size());
            return results;

        } catch (Exception e) {
            logger.error("全文检索异常，回退LIKE搜索", e);
            return fallbackLikeSearch(query, limit);
        }
    }

    @Override
    public void deleteDocuments(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        String sql = "DELETE FROM rag_knowledge WHERE id = ?";
        for (String id : ids) {
            jdbcTemplate.update(sql, Long.parseLong(id));
        }
    }

    /**
     * 将用户查询转换为 MySQL BOOLEAN MODE 查询字符串
     * 例: "你好机器人" → "+你好* +机器人*"
     */
    private String buildBooleanQuery(String query) {
        StringBuilder sb = new StringBuilder();
        for (char c : query.toCharArray()) {
            if (Character.isLetterOrDigit(c) || Character.UnicodeScript.of(c).toString().equals("HAN")) {
                sb.append(c);
            } else {
                sb.append(' ');
            }
        }
        String[] words = sb.toString().trim().split("\\s+");
        if (words.length == 0 || (words.length == 1 && words[0].isEmpty())) {
            return query;
        }
        StringBuilder booleanQuery = new StringBuilder();
        for (String word : words) {
            if (booleanQuery.length() > 0) booleanQuery.append(" ");
            booleanQuery.append("+").append(word).append("*");
        }
        return booleanQuery.toString();
    }

    /**
     * LIKE 模糊匹配兜底（全文索引不可用或低版本 MySQL 时自动降级）
     */
    private List<RagDocument> fallbackLikeSearch(String query, int limit) {
        String sql = "SELECT id, content, source FROM rag_knowledge " +
                "WHERE content LIKE CONCAT('%', ?, '%') " +
                "LIMIT ?";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, query, limit);
        List<RagDocument> results = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            RagDocument doc = new RagDocument();
            doc.setId(String.valueOf(row.get("id")));
            doc.setContent((String) row.get("content"));
            doc.setSource((String) row.get("source"));
            doc.setScore(0.1);
            results.add(doc);
        }
        return results;
    }

    /**
     * 查询某机器人的知识文档列表（去重按 source 分组）
     */
    public List<Map<String, Object>> listByBotId(String botId) {
        String sql = "SELECT source, COUNT(*) AS chunks, " +
                "MAX(create_time) AS update_time, " +
                "SUM(LENGTH(content)) AS total_size " +
                "FROM rag_knowledge WHERE bot_id = ? " +
                "GROUP BY source ORDER BY update_time DESC";
        return jdbcTemplate.queryForList(sql, botId);
    }

    /**
     * 删除某机器人的全部知识文档
     */
    public int deleteByBotId(String botId) {
        String sql = "DELETE FROM rag_knowledge WHERE bot_id = ?";
        return jdbcTemplate.update(sql, botId);
    }

    /**
     * 删除某机器人的单个知识文档（按 source）
     */
    public int deleteByBotIdAndSource(String botId, String source) {
        String sql = "DELETE FROM rag_knowledge WHERE bot_id = ? AND source = ?";
        return jdbcTemplate.update(sql, botId, source);
    }
}