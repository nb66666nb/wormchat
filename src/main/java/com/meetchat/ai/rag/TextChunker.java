package com.meetchat.ai.rag;

import java.util.ArrayList;
import java.util.List;

/**
 * 长文本分块器
 * 按段落切分，相邻块重叠以保持上下文连续性
 */
public class TextChunker {

    /** 每块最大字符数 */
    private final int maxChars;

    /** 相邻块重叠字符数 */
    private final int overlapChars;

    public TextChunker() {
        this(500, 50);
    }

    public TextChunker(int maxChars, int overlapChars) {
        this.maxChars = maxChars;
        this.overlapChars = overlapChars;
    }

    /**
     * 将长文本切分为多个 chunk
     */
    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return chunks;
        }

        // 短文本不切分
        if (text.length() <= maxChars) {
            chunks.add(text.trim());
            return chunks;
        }

        // 先按段落切分
        List<String> paragraphs = splitByParagraph(text);

        StringBuilder current = new StringBuilder();
        for (String para : paragraphs) {
            if (current.length() + para.length() > maxChars && current.length() > 0) {
                chunks.add(current.toString().trim());
                // 重叠: 保留上一块的末尾
                if (overlapChars > 0) {
                    String overlap = current.substring(
                            Math.max(0, current.length() - overlapChars));
                    current = new StringBuilder(overlap);
                } else {
                    current = new StringBuilder();
                }
            }
            if (current.length() > 0) {
                current.append("\n\n");
            }
            current.append(para);
        }

        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }

        return chunks;
    }

    /**
     * 按段落(\n\n)拆分
     */
    private List<String> splitByParagraph(String text) {
        List<String> result = new ArrayList<>();
        String[] paragraphs = text.split("\n\n");
        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.isEmpty()) continue;
            // 如果单个段落仍然太长，按句号再拆
            if (trimmed.length() > maxChars) {
                result.addAll(splitLongParagraph(trimmed));
            } else {
                result.add(trimmed);
            }
        }
        return result;
    }

    /**
     * 超长段落按句号/换行拆分
     */
    private List<String> splitLongParagraph(String paragraph) {
        List<String> result = new ArrayList<>();
        // 先尝试按句号拆
        String[] sentences = paragraph.split("(?<=[。！？])");
        StringBuilder current = new StringBuilder();
        for (String sent : sentences) {
            if (current.length() + sent.length() > maxChars && current.length() > 0) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            }
            current.append(sent);
        }
        if (current.length() > 0) {
            String remaining = current.toString().trim();
            // 如果仍然超长，强制截断
            if (remaining.length() > maxChars) {
                for (int i = 0; i < remaining.length(); i += maxChars) {
                    result.add(remaining.substring(i,
                            Math.min(i + maxChars, remaining.length())));
                }
            } else {
                result.add(remaining);
            }
        }
        return result;
    }
}