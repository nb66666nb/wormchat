package com.meetchat.ai.rag;

import java.util.List;

/**
 * 文档存储接口
 * 抽象向量数据库/搜索引擎的存储层
 * 可对接：Milvus, Pinecone, Elasticsearch, MySQL全文索引等
 */
public interface DocumentStore {

    /**
     * 索引文档
     * @param documents 文档列表
     */
    void indexDocuments(List<RagDocument> documents);

    /**
     * 语义检索
     * @param query 查询文本
     * @param topK 返回前K条结果
     * @return 相关文档列表
     */
    List<RagDocument> search(String query, int topK);

    /**
     * 删除文档
     * @param ids 文档ID列表
     */
    void deleteDocuments(List<String> ids);

    /**
     * 检查存储是否可用
     */
    boolean isAvailable();
}