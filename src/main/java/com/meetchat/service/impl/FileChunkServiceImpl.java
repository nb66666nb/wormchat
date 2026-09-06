package com.meetchat.service.impl;

import com.meetchat.entity.Dto.TokenUserInfoDto;
import com.meetchat.entity.vo.ResponseVO;
import com.meetchat.exception.BusinessException;
import com.meetchat.redis.RedisUtils;
import com.meetchat.service.FileChunkService;
import com.meetchat.utils.StringTools;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.*;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class FileChunkServiceImpl implements FileChunkService {

    private static final Logger logger = LoggerFactory.getLogger(FileChunkServiceImpl.class);

    @Value("${project.folder:./upload}")
    private String projectFolder;

    @Value("${file.chunk.size:5242880}")
    private long defaultChunkSize;

    @Value("${file.chunk.ttl.days:7}")
    private int chunkTtlDays;

    @Value("${file.upload.max-concurrent-per-user:5}")
    private int maxConcurrentPerUser;

    @Value("${file.upload.max-file-size:5368709120}")
    private long maxFileSize;

    private static final String REDIS_KEY_FILE_HASH_PREFIX = "file:hash:";
    private static final String REDIS_KEY_UPLOAD_PREFIX = "file:upload:";
    private static final String REDIS_KEY_HASH_UPLOAD_PREFIX = "file:hash2uid:";
    private static final String REDIS_KEY_USER_UPLOADS_PREFIX = "file:user:uploads:";
    private static final String REDIS_KEY_CHUNK_SET_PREFIX = "file:chunks:";
    private static final long REDIS_TTL_FILE_HASH = 30L * 24 * 60 * 60;
    private static final long REDIS_TTL_UPLOAD = 7L * 24 * 60 * 60;
    private static final String TMP_DIR_NAME = "tmp";
    private static final String FILE_DIR_NAME = "file";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 文件扩展名白名单
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".svg", ".ico",
            ".mp4", ".avi", ".mov", ".wmv", ".flv", ".mkv", ".webm",
            ".mp3", ".wav", ".aac", ".ogg", ".flac", ".wma",
            ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".pdf", ".txt", ".zip", ".rar", ".7z"
    ));

    @Resource
    private RedisUtils<Object> redisUtils;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // ==================== 预检 ====================

    @Override
    public ResponseVO checkUpload(TokenUserInfoDto userInfo, String fileHash, String fileName,
                                  long fileSize, long chunkSize, String meetingNo) {
        if (StringTools.isEmpty(fileHash) || StringTools.isEmpty(fileName) || fileSize <= 0) {
            throw new BusinessException("参数不合法");
        }
        if (fileSize > maxFileSize) {
            throw new BusinessException("文件大小超过限制（最大 " + (maxFileSize / 1024 / 1024) + "MB）");
        }
        validateFileExtension(fileName);
        if (chunkSize <= 0) chunkSize = defaultChunkSize;
        int totalChunks = (int) Math.ceil((double) fileSize / chunkSize);

        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 秒传：Redis 中是否已有相同 fileHash 的完整文件
        Object existingFileObj = redisUtils.get(REDIS_KEY_FILE_HASH_PREFIX + fileHash);
        Map<String, Object> existingFile = castToMap(existingFileObj);
        if (existingFile != null && existingFile.get("fileId") != null) {
            result.put("status", "FAST");
            result.put("fileId", existingFile.get("fileId"));
            result.put("fileName", existingFile.get("fileName"));
            result.put("fileSize", existingFile.get("fileSize"));
            result.put("fileType", existingFile.get("fileType"));
            result.put("filePath", existingFile.get("filePath"));
            result.put("contentType", existingFile.get("contentType"));
            result.put("fileHash", fileHash);
            logger.info("秒传命中 fileHash={}, fileId={}", fileHash, existingFile.get("fileId"));
            return success(result);
        }

        // 2. 断点续传：通过反向索引 file:hash2uid:{fileHash} 快速定位 uploadId
        String resumeUploadId = (String) redisUtils.get(REDIS_KEY_HASH_UPLOAD_PREFIX + fileHash);
        if (resumeUploadId != null) {
            Map<String, Object> uploadMeta = castToMap(redisUtils.get(REDIS_KEY_UPLOAD_PREFIX + resumeUploadId));
            if (uploadMeta != null) {
                // 用户隔离：只有上传者本人才能续传
                String ownerUserId = (String) uploadMeta.get("userId");
                if (!userInfo.getUserId().equals(ownerUserId)) {
                    // 不是本人的上传，当作全新上传处理
                    logger.warn("用户 {} 尝试续传非本人的上传任务 uploadId={}", userInfo.getUserId(), resumeUploadId);
                } else {
                    Set<Object> uploadedChunks = redisTemplate.opsForSet().members(REDIS_KEY_CHUNK_SET_PREFIX + resumeUploadId);
                    List<Integer> uploadedList = new ArrayList<>();
                    if (uploadedChunks != null) {
                        for (Object o : uploadedChunks) {
                            uploadedList.add(Integer.parseInt(o.toString()));
                        }
                        Collections.sort(uploadedList);
                    }
                    int serverTotalChunks = ((Number) uploadMeta.get("totalChunks")).intValue();
                    long serverChunkSize = ((Number) uploadMeta.get("chunkSize")).longValue();
                    result.put("status", "RESUME");
                    result.put("uploadId", resumeUploadId);
                    result.put("chunkSize", serverChunkSize);
                    result.put("totalChunks", serverTotalChunks);
                    result.put("uploadedChunks", uploadedList);
                    result.put("fileHash", fileHash);
                    logger.info("断点续传 uploadId={}, 已上传 {}/{}", resumeUploadId, uploadedList.size(), serverTotalChunks);
                    return success(result);
                }
            }
        }

        // 3. 全新上传：并发数限制
        String userUploadsKey = REDIS_KEY_USER_UPLOADS_PREFIX + userInfo.getUserId();
        Long currentCount = redisTemplate.opsForSet().size(userUploadsKey);
        if (currentCount != null && currentCount >= maxConcurrentPerUser) {
            throw new BusinessException("同时上传任务数已达上限（" + maxConcurrentPerUser + "），请等待完成后再试");
        }

        // 磁盘空间预检
        checkDiskSpace(fileSize);

        String uploadId = UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> uploadMeta = new LinkedHashMap<>();
        uploadMeta.put("fileHash", fileHash);
        uploadMeta.put("fileName", fileName);
        uploadMeta.put("fileSize", fileSize);
        uploadMeta.put("chunkSize", chunkSize);
        uploadMeta.put("totalChunks", totalChunks);
        uploadMeta.put("meetingNo", meetingNo == null ? "" : meetingNo);
        uploadMeta.put("userId", userInfo.getUserId());
        uploadMeta.put("createdAt", System.currentTimeMillis());
        redisUtils.setex(REDIS_KEY_UPLOAD_PREFIX + uploadId, uploadMeta, REDIS_TTL_UPLOAD);

        // 反向索引：fileHash → uploadId（O(1) 查找）
        redisUtils.setex(REDIS_KEY_HASH_UPLOAD_PREFIX + fileHash, uploadId, REDIS_TTL_UPLOAD);

        // 用户上传集合
        redisTemplate.opsForSet().add(userUploadsKey, uploadId);
        redisTemplate.expire(userUploadsKey, REDIS_TTL_UPLOAD, TimeUnit.SECONDS);

        // 创建临时目录
        File uploadTmpDir = new File(projectFolder + "/" + TMP_DIR_NAME, uploadId);
        if (!uploadTmpDir.exists()) {
            uploadTmpDir.mkdirs();
        }

        result.put("status", "NEW");
        result.put("uploadId", uploadId);
        result.put("chunkSize", chunkSize);
        result.put("totalChunks", totalChunks);
        result.put("uploadedChunks", new ArrayList<Integer>());
        result.put("fileHash", fileHash);
        logger.info("全新分片上传 uploadId={}, fileName={}, totalChunks={}", uploadId, fileName, totalChunks);
        return success(result);
    }

    // ==================== 上传分片 ====================

    @Override
    public ResponseVO uploadChunk(TokenUserInfoDto userInfo, byte[] chunkData, String uploadId,
                                  int chunkIndex, String chunkHash) {
        if (StringTools.isEmpty(uploadId)) {
            throw new BusinessException("uploadId 不能为空");
        }

        Map<String, Object> uploadMeta = castToMap(redisUtils.get(REDIS_KEY_UPLOAD_PREFIX + uploadId));
        if (uploadMeta == null) {
            throw new BusinessException("uploadId 不存在或已过期，请重新开始上传");
        }

        // 用户隔离
        String ownerUserId = (String) uploadMeta.get("userId");
        if (!userInfo.getUserId().equals(ownerUserId)) {
            throw new BusinessException("无权操作此上传任务");
        }

        int totalChunks = ((Number) uploadMeta.get("totalChunks")).intValue();
        if (chunkIndex < 0 || chunkIndex >= totalChunks) {
            throw new BusinessException("分片索引越界");
        }

        // 分片 MD5 校验
        if (!StringTools.isEmpty(chunkHash)) {
            String calculated = DigestUtils.md5Hex(chunkData);
            if (!chunkHash.equalsIgnoreCase(calculated)) {
                throw new BusinessException("分片校验失败，MD5 不匹配");
            }
        }

        // 幂等性：已上传的分片直接返回成功
        Boolean isMember = redisTemplate.opsForSet().isMember(REDIS_KEY_CHUNK_SET_PREFIX + uploadId, String.valueOf(chunkIndex));
        if (Boolean.TRUE.equals(isMember)) {
            long uploadedCount = redisTemplate.opsForSet().size(REDIS_KEY_CHUNK_SET_PREFIX + uploadId);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("uploadId", uploadId);
            result.put("chunkIndex", chunkIndex);
            result.put("chunkSize", chunkData.length);
            result.put("uploadedCount", uploadedCount);
            result.put("totalChunks", totalChunks);
            result.put("completed", uploadedCount == totalChunks);
            result.put("idempotent", true);
            return success(result);
        }

        // 写入分片文件
        File uploadTmpDir = new File(projectFolder + "/" + TMP_DIR_NAME, uploadId);
        if (!uploadTmpDir.exists()) {
            uploadTmpDir.mkdirs();
        }
        File chunkFile = new File(uploadTmpDir, String.valueOf(chunkIndex));
        try (FileOutputStream fos = new FileOutputStream(chunkFile)) {
            fos.write(chunkData);
            fos.flush();
            // 强制刷盘，防止 OS 缓存丢失
            fos.getFD().sync();
        } catch (IOException e) {
            throw new BusinessException("分片写入磁盘失败: " + e.getMessage());
        }

        // 原子操作：用 Redis Set 记录已上传分片（SADD 是原子的）
        redisTemplate.opsForSet().add(REDIS_KEY_CHUNK_SET_PREFIX + uploadId, String.valueOf(chunkIndex));
        redisTemplate.expire(REDIS_KEY_CHUNK_SET_PREFIX + uploadId, REDIS_TTL_UPLOAD, TimeUnit.SECONDS);

        long uploadedCount = redisTemplate.opsForSet().size(REDIS_KEY_CHUNK_SET_PREFIX + uploadId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("uploadId", uploadId);
        result.put("chunkIndex", chunkIndex);
        result.put("chunkSize", chunkData.length);
        result.put("uploadedCount", uploadedCount);
        result.put("totalChunks", totalChunks);
        result.put("completed", uploadedCount == totalChunks);
        result.put("idempotent", false);
        return success(result);
    }

    // ==================== 合并分片 ====================

    @Override
    public ResponseVO mergeChunks(TokenUserInfoDto userInfo, String uploadId, String fileHash,
                                  String fileName, int totalChunks, String meetingNo) {
        Map<String, Object> uploadMeta = castToMap(redisUtils.get(REDIS_KEY_UPLOAD_PREFIX + uploadId));
        if (uploadMeta == null) {
            throw new BusinessException("uploadId 不存在或已过期");
        }

        // 用户隔离
        String ownerUserId = (String) uploadMeta.get("userId");
        if (!userInfo.getUserId().equals(ownerUserId)) {
            throw new BusinessException("无权操作此上传任务");
        }

        File uploadTmpDir = new File(projectFolder + "/" + TMP_DIR_NAME, uploadId);
        if (!uploadTmpDir.exists()) {
            throw new BusinessException("分片目录不存在");
        }

        // 校验分片完整性
        Long uploadedCount = redisTemplate.opsForSet().size(REDIS_KEY_CHUNK_SET_PREFIX + uploadId);
        if (uploadedCount == null || uploadedCount != totalChunks) {
            throw new BusinessException("分片不完整，已上传 " + (uploadedCount != null ? uploadedCount : 0) + "/" + totalChunks);
        }

        // 用 FileChannel 零拷贝合并
        File tmpMerged = new File(uploadTmpDir, ".merged.tmp");
        long totalSize;
        try {
            totalSize = mergeChunkFiles(uploadTmpDir, totalChunks, tmpMerged);
        } catch (IOException e) {
            tmpMerged.delete();
            throw new BusinessException("分片合并失败: " + e.getMessage());
        }

        // 整文件 SHA-256 校验
        String mergedHash;
        try {
            mergedHash = calculateSha256(tmpMerged);
        } catch (IOException e) {
            tmpMerged.delete();
            throw new BusinessException("合并后 hash 计算失败: " + e.getMessage());
        }
        if (!fileHash.equalsIgnoreCase(mergedHash)) {
            tmpMerged.delete();
            throw new BusinessException("合并后 hash 不匹配，文件可能损坏");
        }

        // 移动到最终目录
        String fileId = StringTools.getUserRandomId();
        String ext = "";
        if (fileName != null && fileName.contains(".")) {
            ext = fileName.substring(fileName.lastIndexOf("."));
        }
        String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        String finalDir = projectFolder + "/" + FILE_DIR_NAME + "/" + datePath;
        File dir = new File(finalDir);
        if (!dir.exists()) dir.mkdirs();

        String finalFileName = fileId + ext;
        File finalFile = new File(dir, finalFileName);

        // 先尝试 rename（同磁盘 O(1)），失败则拷贝
        if (!tmpMerged.renameTo(finalFile)) {
            try {
                copyFile(tmpMerged, finalFile);
                tmpMerged.delete();
            } catch (IOException e) {
                finalFile.delete();
                throw new BusinessException("文件落盘失败: " + e.getMessage());
            }
        }

        // 写入 Redis 文件索引（用于秒传）
        String contentType = guessContentTypeByExt(ext);
        int fileType = getFileType(contentType, ext);
        String filePath = "/" + FILE_DIR_NAME + "/" + datePath + "/" + finalFileName;

        Map<String, Object> hashInfo = new LinkedHashMap<>();
        hashInfo.put("fileId", fileId);
        hashInfo.put("fileName", fileName);
        hashInfo.put("fileSize", totalSize);
        hashInfo.put("fileType", fileType);
        hashInfo.put("filePath", filePath);
        hashInfo.put("contentType", contentType);
        redisUtils.setex(REDIS_KEY_FILE_HASH_PREFIX + fileHash, hashInfo, REDIS_TTL_FILE_HASH);

        // 清理临时资源
        cleanupUploadResources(uploadId, userInfo.getUserId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fileId", fileId);
        result.put("fileName", fileName);
        result.put("fileSize", totalSize);
        result.put("fileType", fileType);
        result.put("filePath", filePath);
        result.put("contentType", contentType);
        result.put("fileHash", fileHash);

        logger.info("分片合并成功 uploadId={}, fileId={}, size={}", uploadId, fileId, totalSize);
        return success(result);
    }

    // ==================== 取消上传 ====================

    @Override
    public ResponseVO cancelUpload(TokenUserInfoDto userInfo, String uploadId) {
        Map<String, Object> uploadMeta = castToMap(redisUtils.get(REDIS_KEY_UPLOAD_PREFIX + uploadId));
        if (uploadMeta != null) {
            String ownerUserId = (String) uploadMeta.get("userId");
            if (!userInfo.getUserId().equals(ownerUserId)) {
                throw new BusinessException("无权操作此上传任务");
            }
        }
        cleanupUploadResources(uploadId, userInfo.getUserId());
        logger.info("分片上传已取消 uploadId={}", uploadId);
        return success("取消成功");
    }

    // ==================== 定时清理 ====================

    @Override
    public void cleanupStaleChunks() {
        logger.info("[定时任务] 开始清理过期分片目录");
        File tmpDir = new File(projectFolder + "/" + TMP_DIR_NAME);
        if (!tmpDir.exists()) {
            return;
        }
        File[] uploadDirs = tmpDir.listFiles(File::isDirectory);
        if (uploadDirs == null || uploadDirs.length == 0) {
            return;
        }
        long cutoff = System.currentTimeMillis() - (long) chunkTtlDays * 24 * 60 * 60 * 1000L;
        int removed = 0;
        for (File d : uploadDirs) {
            try {
                if (d.lastModified() < cutoff) {
                    deleteDir(d);
                    String uploadId = d.getName();
                    // 清理 Redis：uploadMeta + chunkSet + 反向索引 + 用户集合
                    Map<String, Object> meta = castToMap(redisUtils.get(REDIS_KEY_UPLOAD_PREFIX + uploadId));
                    if (meta != null) {
                        String fileHash = (String) meta.get("fileHash");
                        String userId = (String) meta.get("userId");
                        if (fileHash != null) redisUtils.delete(REDIS_KEY_HASH_UPLOAD_PREFIX + fileHash);
                        if (userId != null) redisTemplate.opsForSet().remove(REDIS_KEY_USER_UPLOADS_PREFIX + userId, uploadId);
                    }
                    redisUtils.delete(REDIS_KEY_UPLOAD_PREFIX + uploadId);
                    redisUtils.delete(REDIS_KEY_CHUNK_SET_PREFIX + uploadId);
                    removed++;
                }
            } catch (Exception e) {
                logger.warn("[定时任务] 删除分片目录失败 {}: {}", d.getName(), e.getMessage());
            }
        }
        logger.info("[定时任务] 清理完成，共移除 {} 个过期分片目录", removed);
    }

    // ==================== 私有工具方法 ====================

    /**
     * FileChannel 零拷贝合并分片
     */
    private long mergeChunkFiles(File uploadTmpDir, int totalChunks, File dest) throws IOException {
        long totalSize = 0;
        try (FileOutputStream fos = new FileOutputStream(dest);
             FileChannel outChannel = fos.getChannel()) {
            for (int i = 0; i < totalChunks; i++) {
                File chunk = new File(uploadTmpDir, String.valueOf(i));
                if (!chunk.exists()) {
                    throw new IOException("分片 " + i + " 不存在");
                }
                try (FileInputStream fis = new FileInputStream(chunk);
                     FileChannel inChannel = fis.getChannel()) {
                    long transferred = outChannel.transferFrom(inChannel, outChannel.size(), inChannel.size());
                    totalSize += transferred;
                }
            }
            fos.getFD().sync();
        }
        return totalSize;
    }

    /**
     * 文件拷贝（rename 失败时的 fallback）
     */
    private void copyFile(File src, File dest) throws IOException {
        try (FileInputStream fis = new FileInputStream(src);
             FileOutputStream fos = new FileOutputStream(dest);
             FileChannel inChannel = fis.getChannel();
             FileChannel outChannel = fos.getChannel()) {
            outChannel.transferFrom(inChannel, 0, inChannel.size());
            outChannel.force(true);
        }
    }

    /**
     * 清理单个上传任务的所有 Redis 键 + 临时目录
     */
    private void cleanupUploadResources(String uploadId, String userId) {
        // 清理反向索引
        Map<String, Object> meta = castToMap(redisUtils.get(REDIS_KEY_UPLOAD_PREFIX + uploadId));
        if (meta != null) {
            String fileHash = (String) meta.get("fileHash");
            if (fileHash != null) {
                redisUtils.delete(REDIS_KEY_HASH_UPLOAD_PREFIX + fileHash);
            }
            String metaUserId = (String) meta.get("userId");
            if (metaUserId != null) {
                redisTemplate.opsForSet().remove(REDIS_KEY_USER_UPLOADS_PREFIX + metaUserId, uploadId);
            }
        }
        // 清理 uploadMeta / chunkSet
        redisUtils.delete(REDIS_KEY_UPLOAD_PREFIX + uploadId);
        redisUtils.delete(REDIS_KEY_CHUNK_SET_PREFIX + uploadId);
        // 清理磁盘
        File uploadTmpDir = new File(projectFolder + "/" + TMP_DIR_NAME, uploadId);
        if (uploadTmpDir.exists()) {
            deleteDir(uploadTmpDir);
        }
    }

    /**
     * 磁盘空间预检
     */
    private void checkDiskSpace(long requiredBytes) {
        File partition = new File(projectFolder);
        if (!partition.exists()) {
            partition = partition.getParentFile();
        }
        if (partition != null) {
            long freeSpace = partition.getFreeSpace();
            // 预留 500MB 给系统
            long buffer = 500L * 1024 * 1024;
            if (freeSpace < requiredBytes + buffer) {
                throw new BusinessException("服务器磁盘空间不足，无法接收文件");
            }
        }
    }

    /**
     * 文件扩展名白名单校验
     */
    private void validateFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new BusinessException("文件名缺少扩展名");
        }
        String ext = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException("不支持的文件类型: " + ext);
        }
    }

    private String calculateSha256(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = fis.read(buf)) != -1) {
                    digest.update(buf, 0, len);
                }
            }
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IOException("SHA-256 计算失败: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castToMap(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        try {
            return objectMapper.convertValue(obj, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private static void deleteDir(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDir(f);
                } else {
                    f.delete();
                }
            }
        }
        dir.delete();
    }

    private int getFileType(String contentType, String ext) {
        String lowerExt = ext.toLowerCase();
        if (lowerExt.matches("\\.(jpg|jpeg|png|gif|bmp|webp|svg|ico)")) return 1;
        if (lowerExt.matches("\\.(mp4|avi|mov|wmv|flv|mkv|webm)")) return 2;
        if (lowerExt.matches("\\.(mp3|wav|aac|ogg|flac|wma)")) return 3;
        if (lowerExt.matches("\\.(doc|docx|xls|xlsx|ppt|pptx|pdf|txt)")) return 4;
        return 5;
    }

    private String guessContentTypeByExt(String ext) {
        Map<String, String> map = new HashMap<>();
        map.put(".jpg", "image/jpeg"); map.put(".jpeg", "image/jpeg");
        map.put(".png", "image/png"); map.put(".gif", "image/gif");
        map.put(".webp", "image/webp"); map.put(".svg", "image/svg+xml");
        map.put(".mp4", "video/mp4"); map.put(".webm", "video/webm");
        map.put(".mp3", "audio/mpeg"); map.put(".wav", "audio/wav");
        map.put(".pdf", "application/pdf"); map.put(".txt", "text/plain");
        map.put(".zip", "application/zip"); map.put(".rar", "application/x-rar-compressed");
        return map.getOrDefault(ext == null ? "" : ext.toLowerCase(), "application/octet-stream");
    }

    private ResponseVO success(Object data) {
        ResponseVO vo = new ResponseVO();
        vo.setStatus("success");
        vo.setCode(200);
        vo.setInfo("操作成功");
        vo.setData(data);
        return vo;
    }
}