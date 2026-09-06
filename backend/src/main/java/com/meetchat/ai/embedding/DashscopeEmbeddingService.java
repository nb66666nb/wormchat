package com.meetchat.ai.embedding;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.meetchat.ai.llm.LlmClient;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 通义千问Embedding服务实现
 * 使用阿里DashScope API生成文本向量
 */
@Service("dashscopeEmbeddingService")
public class DashscopeEmbeddingService implements EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(DashscopeEmbeddingService.class);

    /** 通义千问Embedding模型 */
    private static final String MODEL = "text-embedding-v2";
    /** 最大输入Token数 */
    private static final int MAX_TOKENS = 2048;
    /** 批量API最大条数 */
    private static final int BATCH_MAX_SIZE = 25;

    @Value("${ai.llm.api-key:}")
    private String apiKey;

    @Value("${ai.llm.base-url:https://dashscope.aliyuncs.com/api/v1}")
    private String baseUrl;

    @Resource
    private LlmClient llmClient;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    @Override
    public EmbeddingResult embed(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        // 截断过长的文本
        if (text.length() > MAX_TOKENS * 4) {
            text = text.substring(0, MAX_TOKENS * 4);
        }

        try {
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", MODEL);
            JSONObject input = new JSONObject();
            input.put("texts", Collections.singletonList(text));
            requestBody.put("input", input);

            String responseBody = callEmbeddingApi(requestBody);
            if (responseBody == null) {
                return null;
            }

            JSONObject json = JSON.parseObject(responseBody);
            JSONArray embeddings = json.getJSONObject("output")
                    .getJSONArray("embeddings");
            if (embeddings != null && !embeddings.isEmpty()) {
                JSONArray vectorArray = embeddings.getJSONObject(0).getJSONArray("embedding");
                float[] vector = new float[vectorArray.size()];
                for (int i = 0; i < vectorArray.size(); i++) {
                    vector[i] = vectorArray.getFloatValue(i);
                }
                return new EmbeddingResult(vector);
            }
        } catch (Exception e) {
            logger.warn("Embedding调用异常", e);
        }

        return null;
    }

    @Override
    public List<EmbeddingResult> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        List<EmbeddingResult> results = new ArrayList<>(texts.size());

        // 分批处理，每批最多BATCH_MAX_SIZE条
        for (int batchStart = 0; batchStart < texts.size(); batchStart += BATCH_MAX_SIZE) {
            int batchEnd = Math.min(batchStart + BATCH_MAX_SIZE, texts.size());
            List<String> batch = texts.subList(batchStart, batchEnd);

            // 截断过长文本
            List<String> truncatedBatch = new ArrayList<>(batch.size());
            for (String text : batch) {
                if (text == null || text.trim().isEmpty()) {
                    truncatedBatch.add("");
                } else if (text.length() > MAX_TOKENS * 4) {
                    truncatedBatch.add(text.substring(0, MAX_TOKENS * 4));
                } else {
                    truncatedBatch.add(text);
                }
            }

            // 批量API调用
            List<EmbeddingResult> batchResults = callBatchEmbedding(truncatedBatch);
            results.addAll(batchResults);

            logger.debug("批量Embedding进度: {}/{}", batchEnd, texts.size());
        }

        return results;
    }

    /**
     * 批量Embedding API调用
     */
    private List<EmbeddingResult> callBatchEmbedding(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", MODEL);
            JSONObject input = new JSONObject();
            input.put("texts", texts);
            requestBody.put("input", input);

            String responseBody = callEmbeddingApi(requestBody);
            if (responseBody == null) {
                return Collections.emptyList();
            }

            JSONObject json = JSON.parseObject(responseBody);
            JSONArray embeddings = json.getJSONObject("output")
                    .getJSONArray("embeddings");

            List<EmbeddingResult> results = new ArrayList<>();
            if (embeddings != null) {
                for (int i = 0; i < embeddings.size(); i++) {
                    JSONArray vectorArray = embeddings.getJSONObject(i).getJSONArray("embedding");
                    if (vectorArray != null) {
                        float[] vector = new float[vectorArray.size()];
                        for (int j = 0; j < vectorArray.size(); j++) {
                            vector[j] = vectorArray.getFloatValue(j);
                        }
                        results.add(new EmbeddingResult(vector));
                    } else {
                        results.add(null);
                    }
                }
            }
            return results;
        } catch (Exception e) {
            logger.warn("批量Embedding调用异常", e);
            return Collections.emptyList();
        }
    }

    /**
     * 调用Embedding API的统一方法
     */
    private String callEmbeddingApi(JSONObject requestBody) throws IOException {
        Request request = new Request.Builder()
                .url(baseUrl + "/embeddings")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(requestBody.toJSONString(), MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.warn("Embedding API调用失败: {}", response.code());
                return null;
            }
            return response.body() != null ? response.body().string() : null;
        }
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}
