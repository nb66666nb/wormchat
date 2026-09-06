package com.meetchat.ai.rag;

/**
 * 纯文本 / Markdown 解析器
 */
public class TxtParser {

    /**
     * 读取文本内容（txt/md 等纯文本文件直接返回）
     */
    public String parse(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return "";
        }
        return rawText.trim();
    }
}