package com.meetchat.ai.rag;

import com.meetchat.ai.embedding.EmbeddingResult;
import com.meetchat.ai.embedding.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合检索RAG服务
 * 结合向量语义检索 + MySQL全文关键词检索，提升召回率
 *
 * 检索策略：
 * 1. 向量检索：通过Embedding将查询转为向量，在Milvus中检索相似文档
 * 2. 关键词检索：通过MySQL FULLTEXT进行关键词匹配
 * 3. 结果融合：对两种检索结果进行去重、加权合并
 */
@Primary
@Service("hybridRagService")
public class HybridRagService implements RagService, DocumentStore {

    private static final Logger logger = LoggerFactory.getLogger(HybridRagService.class);

    /** 向量检索返回数量 */
    private static final int VECTOR_TOP_K = 5;
    /** 关键词检索返回数量 */
    private static final int KEYWORD_TOP_K = 5;
    /** 最终返回最大数量 */
    private static final int MAX_RESULTS = 3;
    /** 向量检索权重 */
    private static final double VECTOR_WEIGHT = 0.7;
    /** 关键词检索权重 */
    private static final double KEYWORD_WEIGHT = 0.3;

    @Resource
    private EmbeddingService embeddingService;

    @Resource(name = "milvusVectorStore")
    private VectorStore vectorStore;

    @Resource(name = "mysqlDocumentStore")
    private DocumentStore keywordDocumentStore;

    @Resource(name = "milvusVectorStore")
    private MilvusVectorStore milvusVectorStore;

    @Value("${rag.vector.enabled:true}")
    private boolean vectorEnabled;

    @Value("${rag.keyword.enabled:true}")
    private boolean keywordEnabled;

    @Override
    public List<RagDocument> retrieve(String query, int topK) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        int effectiveTopK = Math.min(topK, MAX_RESULTS);
        List<RagDocument> results = new ArrayList<>();

        try {
            // 1. 向量检索
            List<RagDocument> vectorResults = Collections.emptyList();
            if (vectorEnabled && embeddingService.isAvailable() && vectorStore.isAvailable()) {
                vectorResults = vectorSearch(query, VECTOR_TOP_K);
            }

            // 2. 关键词检索
            List<RagDocument> keywordResults = Collections.emptyList();
            if (keywordEnabled && keywordDocumentStore != null && keywordDocumentStore.isAvailable()) {
                keywordResults = keywordDocumentStore.search(query, KEYWORD_TOP_K);
            }

            // 3. 融合排序
            results = mergeAndRank(vectorResults, keywordResults, effectiveTopK);

            logger.info("混合RAG检索: query={}, results={}", query, results.size());

        } catch (Exception e) {
            logger.error("混合RAG检索异常", e);
        }

        return results;
    }

    /**
     * 向量检索
     */
    private List<RagDocument> vectorSearch(String query, int topK) {
        try {
            EmbeddingResult embedding = embeddingService.embed(query);
            if (embedding == null || embedding.getVector() == null) {
                logger.warn("Embedding生成失败");
                return Collections.emptyList();
            }

            List<RetrievedChunk> chunks = vectorStore.search(embedding.getVector(), topK);
            if (chunks.isEmpty()) {
                return Collections.emptyList();
            }

            List<RagDocument> docs = new ArrayList<>();
            for (RetrievedChunk chunk : chunks) {
                RagDocument doc = new RagDocument();
                doc.setId(chunk.getDocId());
                doc.setContent(chunk.getText());
                doc.setSource(chunk.getTitle());
                doc.setScore(1.0 - chunk.getScore()); // Milvus返回的是距离，转换为相似度
                docs.add(doc);
            }

            logger.debug("向量检索返回{}条结果", docs.size());
            return docs;

        } catch (Exception e) {
            logger.warn("向量检索失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 融合两种检索结果
     */
    private List<RagDocument> mergeAndRank(List<RagDocument> vectorResults,
                                            List<RagDocument> keywordResults,
                                            int topK) {
        // 使用Map按ID去重，记录向量/关键词得分
        Map<String, Double> scoreMap = new HashMap<>();

        for (RagDocument doc : vectorResults) {
            String id = doc.getId();
            if (id != null) {
                scoreMap.put(id, scoreMap.getOrDefault(id, 0.0) + doc.getScore() * VECTOR_WEIGHT);
            }
        }

        for (RagDocument doc : keywordResults) {
            String id = doc.getId();
            if (id != null) {
                scoreMap.put(id, scoreMap.getOrDefault(id, 0.0) + (doc.getScore() != null ? doc.getScore() : 0.5) * KEYWORD_WEIGHT);
            }
        }

        // 按得分排序
        List<Map.Entry<String, Double>> sortedList = scoreMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .collect(Collectors.toList());

        // 构建结果
        List<RagDocument> mergedResults = new ArrayList<>();
        for (Map.Entry<String, Double> entry : sortedList) {
            RagDocument matched = null;
            for (RagDocument doc : vectorResults) {
                if (entry.getKey().equals(doc.getId())) {
                    matched = doc;
                    break;
                }
            }
            if (matched == null) {
                for (RagDocument doc : keywordResults) {
                    if (entry.getKey().equals(doc.getId())) {
                        matched = doc;
                        break;
                    }
                }
            }

            if (matched != null) {
                RagDocument result = new RagDocument();
                result.setId(matched.getId());
                result.setContent(matched.getContent());
                result.setSource(matched.getSource());
                result.setScore(entry.getValue());
                mergedResults.add(result);
            }
        }

        return mergedResults;
    }

    @Override
    public String formatContext(List<RagDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            RagDocument doc = documents.get(i);
            sb.append("【资料").append(i + 1).append("】");
            if (doc.getSource() != null) {
                sb.append("(来源: ").append(doc.getSource()).append(")");
            }
            sb.append("\n").append(doc.getContent()).append("\n\n");
        }
        return sb.toString();
    }

    @Override
    public void indexDocuments(List<RagDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }

        // 1. 同步索引到MySQL（关键词检索，需要立即可用）
        if (keywordEnabled && keywordDocumentStore != null && keywordDocumentStore.isAvailable()) {
            keywordDocumentStore.indexDocuments(documents);
        }

        // 2. 异步索引到Milvus（向量检索，后台处理不阻塞导入）
        if (vectorEnabled && milvusVectorStore != null && milvusVectorStore.isAvailable()) {
            List<String> docIds = new ArrayList<>();
            List<String> titles = new ArrayList<>();
            List<String> contents = new ArrayList<>();

            for (int i = 0; i < documents.size(); i++) {
                RagDocument doc = documents.get(i);
                docIds.add(doc.getId() != null ? doc.getId() : String.valueOf(System.currentTimeMillis() + i));
                titles.add(doc.getSource() != null ? doc.getSource() : "unknown");
                contents.add(doc.getContent());
            }

            // 提交到Milvus异步线程池，立即返回
            milvusVectorStore.addDocumentsBatch(docIds, titles, contents);
            logger.info("文档索引已提交异步处理: count={}", documents.size());
        }
    }

    @Override
    public boolean isEnabled() {
        return vectorEnabled || keywordEnabled;
    }

    /**
     * DocumentStore 接口要求：语义检索
     * 委托给 retrieve 方法
     */
    @Override
    public List<RagDocument> search(String query, int topK) {
        return retrieve(query, topK);
    }

    /**
     * DocumentStore 接口要求：删除文档
     * 同时删除 MySQL 和 Milvus 中的数据
     */
    @Override
    public void deleteDocuments(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        // 1. 删除 MySQL
        if (keywordEnabled && keywordDocumentStore != null && keywordDocumentStore.isAvailable()) {
            try {
                keywordDocumentStore.deleteDocuments(ids);
            } catch (Exception e) {
                logger.warn("删除MySQL文档失败", e);
            }
        }

        // 2. 删除 Milvus
        if (vectorEnabled && vectorStore != null && vectorStore.isAvailable()) {
            for (String id : ids) {
                try {
                    vectorStore.deleteDocument(id);
                } catch (Exception e) {
                    logger.warn("删除Milvus文档失败: id={}", id, e);
                }
            }
        }
    }

    /**
     * DocumentStore 接口要求：检查存储是否可用
     */
    @Override
    public boolean isAvailable() {
        boolean keywordAvailable = keywordEnabled && keywordDocumentStore != null && keywordDocumentStore.isAvailable();
        boolean vectorAvailable = vectorEnabled && vectorStore != null && vectorStore.isAvailable();
        return keywordAvailable || vectorAvailable;
    }
}
