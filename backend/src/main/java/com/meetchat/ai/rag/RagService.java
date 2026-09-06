package com.meetchat.ai.rag;

import java.util.List;

/**
 * RAG检索增强生成服务接口
 */
public interface RagService {

    /**
     * 检索与查询相关的文档
     * @param query 用户查询
     * @param topK 返回前K条
     * @return 相关文档列表
     */
    List<RagDocument> retrieve(String query, int topK);

    /**
     * 将检索结果格式化为上下文字符串
     * @param documents 检索到的文档
     * @return 格式化后的上下文
     */
    String formatContext(List<RagDocument> documents);

    /**
     * 索引文档到知识库
     * @param documents 文档列表
     */
    void indexDocuments(List<RagDocument> documents);

    /**
     * RAG是否启用
     */
    boolean isEnabled();
}