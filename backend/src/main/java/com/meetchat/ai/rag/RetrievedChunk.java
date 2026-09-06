package com.meetchat.ai.rag;

import java.util.List;

/**
 * 文档检索结果
 */
public class RetrievedChunk {

    private String text;
    private String docId;
    private String title;
    private float score;

    public RetrievedChunk() {}

    public RetrievedChunk(String text, String docId, String title, float score) {
        this.text = text;
        this.docId = docId;
        this.title = title;
        this.score = score;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }
}
