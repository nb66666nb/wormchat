package com.meetchat.ai.embedding;

import java.util.List;

/**
 * Embedding服务接口
 * 将文本转换为向量表示
 */
public interface EmbeddingService {

    /**
     * 获取单条文本的向量表示
     * @param text 输入文本
     * @return 向量结果
     */
    EmbeddingResult embed(String text);

    /**
     * 批量获取文本的向量表示
     * @param texts 输入文本列表
     * @return 向量结果列表
     */
    List<EmbeddingResult> embedBatch(List<String> texts);

    /**
     * 检查服务是否可用
     */
    boolean isAvailable();
}
