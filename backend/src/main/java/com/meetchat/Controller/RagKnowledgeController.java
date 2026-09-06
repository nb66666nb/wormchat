package com.meetchat.Controller;

import com.meetchat.ai.rag.*;
import com.meetchat.entity.vo.ResponseVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.*;

/**
 * RAG 知识库管理接口
 *
 * POST /rag/upload       — 上传文件（multipart），自动解析+分块+索引
 * POST /rag/import       — 手动导入文本
 * GET  /rag/list         — 查询某机器人的知识文档列表
 * GET  /rag/search       — 检索
 * DELETE /rag/bot/{botId} — 清空机器人知识
 * DELETE /rag/{id}       — 删除单条
 * GET  /rag/status       — 可用性检查
 */
@RestController
@RequestMapping("/rag")
public class RagKnowledgeController extends ABaseController {

    private static final Logger logger = LoggerFactory.getLogger(RagKnowledgeController.class);

    @Resource(name = "hybridRagService")
    private DocumentStore documentStore;

    @Resource
    private MysqlDocumentStore mysqlDocumentStore;

    @Resource
    private DocumentParserService documentParserService;

    /** 单文件最大 10MB */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * 上传文件并自动索引
     * 支持 .txt .json .md 格式
     */
    @PostMapping("/upload")
    public ResponseVO upload(@RequestParam("file") MultipartFile file,
                             @RequestParam("botId") String botId) {
        if (file.isEmpty()) {
            return getServerErrorResponseVO("文件为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            return getServerErrorResponseVO("文件过大，最大支持 10MB");
        }

        try {
            byte[] bytes = file.getBytes();
            String fileName = file.getOriginalFilename();
            if (fileName == null) fileName = "unknown";

            // 1. 解析 + 分块
            List<String> chunks = documentParserService.parseAndChunk(bytes, fileName);
            if (chunks.isEmpty()) {
                return getServerErrorResponseVO("文件内容为空或无法解析");
            }

            // 2. 索引到 rag_knowledge
            List<RagDocument> docs = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                RagDocument doc = new RagDocument();
                doc.setContent(chunks.get(i));
                doc.setSource(fileName);
                doc.setBotId(botId);
                doc.setChunkIndex(i);
                docs.add(doc);
            }
            documentStore.indexDocuments(docs);

            logger.info("文件上传索引成功: botId={}, file={}, chunks={}, size={}B",
                    botId, fileName, chunks.size(), bytes.length);

            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "ok");
            resp.put("fileName", fileName);
            resp.put("chunks", chunks.size());
            resp.put("size", bytes.length);
            return getSuccessResponseVO(resp);

        } catch (Exception e) {
            logger.error("文件上传失败: botId={}", botId, e);
            return getServerErrorResponseVO("上传失败: " + e.getMessage());
        }
    }

    /**
     * 导入单条知识（手动输入文本）
     */
    @PostMapping("/import")
    public ResponseVO importDocument(@RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.trim().isEmpty()) {
            return getServerErrorResponseVO("内容不能为空");
        }
        String botId = body.get("botId");
        String source = body.getOrDefault("source", "manual");

        // 手动导入也走分块
        List<String> chunks = documentParserService.parseAndChunk(
                content.getBytes(java.nio.charset.StandardCharsets.UTF_8), source);

        List<RagDocument> docs = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            RagDocument doc = new RagDocument();
            doc.setContent(chunks.get(i));
            doc.setSource(source);
            doc.setBotId(botId);
            doc.setChunkIndex(i);
            docs.add(doc);
        }
        documentStore.indexDocuments(docs);

        logger.info("RAG 文本导入: botId={}, source={}, chunks={}", botId, source, chunks.size());
        return getSuccessResponseVO(map("status", "ok", "chunks", chunks.size()));
    }

    /**
     * 查询某机器人的知识文档列表
     * POST /rag/list  (body: {"botId":"xxx"})
     */
    @PostMapping("/list")
    public ResponseVO list(@RequestBody Map<String, String> body) {
        String botId = body.get("botId");
        if (botId == null || botId.isEmpty()) {
            return getServerErrorResponseVO("botId 不能为空");
        }
        List<Map<String, Object>> docs = mysqlDocumentStore.listByBotId(botId);
        return getSuccessResponseVO(docs);
    }

    /**
     * 检索
     * GET /rag/search?q=视频会议
     */
    @GetMapping("/search")
    public ResponseVO search(@RequestParam("q") String query) {
        if (query == null || query.trim().isEmpty()) {
            return getServerErrorResponseVO("搜索词不能为空");
        }
        List<RagDocument> results = documentStore.search(query, 5);
        return getSuccessResponseVO(results);
    }

    /**
     * 删除单条
     */
    @DeleteMapping("/{id}")
    public ResponseVO delete(@PathVariable Long id) {
        List<String> ids = new ArrayList<>();
        ids.add(String.valueOf(id));
        documentStore.deleteDocuments(ids);
        logger.info("RAG 删除: id={}", id);
        return getSuccessResponseVO(map("status", "ok", "id", id));
    }

    /**
     * 清空某机器人的全部知识
     * POST /rag/clear  (body: {"botId":"R123456"})
     */
    @PostMapping("/clear")
    public ResponseVO clearBot(@RequestBody Map<String, String> body) {
        String botId = body.get("botId");
        if (botId == null || botId.isEmpty()) {
            return getServerErrorResponseVO("botId 不能为空");
        }
        int deleted = mysqlDocumentStore.deleteByBotId(botId);
        logger.info("RAG 清空机器人知识: botId={}, deleted={}", botId, deleted);
        return getSuccessResponseVO(map("status", "ok", "deleted", deleted));
    }

    @PostMapping("/delete-one")
    public ResponseVO deleteOne(@RequestBody Map<String, String> body) {
        String botId = body.get("botId");
        String source = body.get("source");
        if (botId == null || botId.isEmpty() || source == null || source.isEmpty()) {
            return getServerErrorResponseVO("botId 和 source 不能为空");
        }
        int deleted = mysqlDocumentStore.deleteByBotIdAndSource(botId, source);
        logger.info("RAG 删除文档: botId={}, source={}, deleted={}", botId, source, deleted);
        return getSuccessResponseVO(map("status", "ok", "deleted", deleted));
    }

    /**
     * 检查 RAG 可用性
     */
    @GetMapping("/status")
    public ResponseVO status() {
        boolean available = mysqlDocumentStore.isAvailable();
        return getSuccessResponseVO(map("available", available));
    }

    /** Java 8 兼容的 Map.of() 替代 */
    private Map<String, Object> map(String k1, Object v1) {
        Map<String, Object> m = new HashMap<>();
        m.put(k1, v1);
        return m;
    }
    private Map<String, Object> map(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> m = new HashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }
}