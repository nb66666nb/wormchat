package com.meetchat.ai.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档解析服务
 * 根据文件扩展名路由到不同解析器，处理文本提取和分块
 */
@Service("documentParserService")
public class DocumentParserService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentParserService.class);

    private final TxtParser txtParser = new TxtParser();
    private final JsonChatLogParser jsonParser = new JsonChatLogParser();
    private final TextChunker chunker = new TextChunker(500, 50);

    /**
     * 解析文件内容并分块
     * @param fileBytes 文件字节
     * @param fileName 文件名（用于判断扩展名）
     * @return 分块后的文本列表
     */
    public List<String> parseAndChunk(byte[] fileBytes, String fileName) {
        String rawText = new String(fileBytes, StandardCharsets.UTF_8);
        String parsed;

        String ext = getExtension(fileName).toLowerCase();
        switch (ext) {
            case "json":
                if (jsonParser.isChatLog(rawText)) {
                    parsed = jsonParser.parse(rawText);
                } else {
                    parsed = rawText;
                }
                break;
            case "txt":
            case "md":
            case "markdown":
            default:
                parsed = txtParser.parse(rawText);
                break;
        }

        if (parsed.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> chunks = chunker.chunk(parsed);
        logger.info("文档解析完成: fileName={}, size={}B, chunks={}",
                fileName, fileBytes.length, chunks.size());

        return chunks;
    }

    /**
     * 获取文件扩展名
     */
    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
}