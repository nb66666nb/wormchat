package com.meetchat.ai.rag;

import com.meetchat.ai.config.AiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RAG服务实现
 * 当前为本地内存实现，可扩展对接向量数据库
 */
@Service("ragService")
public class RagServiceImpl implements RagService {

    private static final Logger logger = LoggerFactory.getLogger(RagServiceImpl.class);

    @Resource
    private AiConfig aiConfig;

    @Autowired(required = false)
    private DocumentStore documentStore;

    @Override
    public List<RagDocument> retrieve(String query, int topK) {
        if (!isEnabled()) {
            return Collections.emptyList();
        }

        if (documentStore == null || !documentStore.isAvailable()) {
            logger.warn("DocumentStore不可用，跳过RAG检索");
            return Collections.emptyList();
        }

        try {
            List<RagDocument> results = documentStore.search(query, topK);
            logger.info("RAG检索: query={}, topK={}, results={}", query, topK, results.size());
            return results;
        } catch (Exception e) {
            logger.error("RAG检索异常", e);
            return Collections.emptyList();
        }
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
        if (!isEnabled() || documentStore == null || !documentStore.isAvailable()) {
            logger.warn("RAG或DocumentStore不可用，跳过索引");
            return;
        }
        documentStore.indexDocuments(documents);
    }

    @Override
    public boolean isEnabled() {
        return aiConfig.getRagEnabled() != null && aiConfig.getRagEnabled();
    }
}