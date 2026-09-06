package com.meetchat.ai.rag;

/**
 * RAG文档
 */
public class RagDocument {

    private String id;
    private String content;
    private String source;
    private Double score;
    private String botId;
    private Integer chunkIndex;

    public RagDocument() {}

    public RagDocument(String id, String content, String source, Double score) {
        this.id = id;
        this.content = content;
        this.source = source;
        this.score = score;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public String getBotId() { return botId; }
    public void setBotId(String botId) { this.botId = botId; }
    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }
}