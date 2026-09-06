package com.meetchat.service;

import com.meetchat.entity.Dto.TokenUserInfoDto;
import com.meetchat.entity.vo.ResponseVO;

import java.util.Map;

/**
 * 分片上传业务接口
 */
public interface FileChunkService {

    /**
     * 分片上传预检（秒传 / 断点续传 / 全新）
     */
    ResponseVO checkUpload(TokenUserInfoDto userInfo, String fileHash, String fileName,
                           long fileSize, long chunkSize, String meetingNo);

    /**
     * 上传单个分片
     */
    ResponseVO uploadChunk(TokenUserInfoDto userInfo, byte[] chunkData, String uploadId,
                           int chunkIndex, String chunkHash);

    /**
     * 合并分片
     */
    ResponseVO mergeChunks(TokenUserInfoDto userInfo, String uploadId, String fileHash,
                           String fileName, int totalChunks, String meetingNo);

    /**
     * 取消上传
     */
    ResponseVO cancelUpload(TokenUserInfoDto userInfo, String uploadId);

    /**
     * 清理过期分片（定时任务调用）
     */
    void cleanupStaleChunks();
}