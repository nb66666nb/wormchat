package com.meetchat.ai.embedding;

/**
 * Embedding结果
 */
public class EmbeddingResult {

    private float[] vector;

    public EmbeddingResult(float[] vector) {
        this.vector = vector;
    }

    public float[] getVector() {
        return vector;
    }
}
