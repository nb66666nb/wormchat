package com.meetchat.ai.rag;

import java.util.List;

/**
 * 向量检索接口
 * 支持多种向量数据库后端（Milvus、Qdrant等）
 */
public interface VectorStore {

    /**
     * 添加文档到向量库
     * @param docId 文档ID
     * @param title 文档标题
     * @param content 文档内容
     */
    void addDocument(String docId, String title, String content);

    /**
     * 删除文档
     * @param docId 文档ID
     */
    void deleteDocument(String docId);

    /**
     * 向量检索
     * @param queryVector 查询向量
     * @param topK 返回结果数量
     * @return 检索结果
     */
    List<RetrievedChunk> search(float[] queryVector, int topK);

    /**
     * 检查向量库是否可用
     */
    boolean isAvailable();
}
