package com.meetchat.ai.llm;

import com.alibaba.fastjson.JSON;
import com.meetchat.ai.config.AiConfig;
import com.meetchat.utils.StringTools;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI兼容客户端实现
 * 支持所有OpenAI API兼容的提供商：
 * - OpenAI (GPT-4o, GPT-4o-mini, etc.)
 * - Azure OpenAI
 * - DeepSeek
 * - 通义千问 (DashScope)
 * - 本地部署 (Ollama, vLLM, LocalAI)
 */
@Component("openAiClient")
public class OpenAiClient implements LlmClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiClient.class);

    @Resource
    private AiConfig aiConfig;

    private volatile OkHttpClient httpClient;

    private OkHttpClient getHttpClient() {
        if (httpClient == null) {
            synchronized (this) {
                if (httpClient == null) {
                    httpClient = new OkHttpClient.Builder()
                            .connectTimeout(aiConfig.getTimeoutSeconds(), TimeUnit.SECONDS)
                            .readTimeout(aiConfig.getTimeoutSeconds(), TimeUnit.SECONDS)
                            .writeTimeout(aiConfig.getTimeoutSeconds(), TimeUnit.SECONDS)
                            .build();
                }
            }
        }
        return httpClient;
    }

    @Override
    public LlmResponse chat(List<LlmRequest.Message> messages) {
        return chat(messages, aiConfig.getTemperature(), aiConfig.getMaxTokens());
    }

    @Override
    public LlmResponse chat(List<LlmRequest.Message> messages, Double temperature, Integer maxTokens) {
        if (!isAvailable()) {
            throw new RuntimeException("LLM服务未配置或不可用");
        }

        LlmRequest request = LlmRequest.create(
                aiConfig.getModel(),
                messages,
                temperature != null ? temperature : aiConfig.getTemperature(),
                maxTokens != null ? maxTokens : aiConfig.getMaxTokens()
        );

        String url = aiConfig.getBaseUrl() + "/chat/completions";
        String jsonBody = JSON.toJSONString(request);

        logger.info("LLM请求: url={}, model={}, messagesCount={}", url, request.getModel(), messages.size());
        logger.debug("LLM请求体: {}", jsonBody);

        Request httpRequest = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + aiConfig.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json; charset=utf-8")))
                .build();

        try (Response response = getHttpClient().newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "无响应体";
                logger.error("LLM请求失败: code={}, body={}", response.code(), errorBody);
                throw new RuntimeException("LLM请求失败: HTTP " + response.code());
            }

            String responseBody = response.body().string();
            logger.debug("LLM响应体: {}", responseBody);
            LlmResponse llmResponse = JSON.parseObject(responseBody, LlmResponse.class);
            logger.info("LLM响应成功: model={}, tokens={}",
                    llmResponse.getModel(),
                    llmResponse.getUsage() != null ? llmResponse.getUsage().getTotalTokens() : "unknown");
            return llmResponse;
        } catch (IOException e) {
            logger.error("LLM请求异常", e);
            throw new RuntimeException("LLM请求异常: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        return !StringTools.isEmpty(aiConfig.getApiKey()) && !StringTools.isEmpty(aiConfig.getBaseUrl());
    }
}