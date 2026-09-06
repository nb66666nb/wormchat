package com.meetchat.Controller;

import com.meetchat.annotation.GlobalInterceptor;
import com.meetchat.entity.Dto.TokenUserInfoDto;
import com.meetchat.entity.contants.Contants;
import com.meetchat.entity.vo.ResponseVO;
import com.meetchat.service.FileChunkService;
import com.meetchat.utils.StringTools;
import com.mysql.cj.Constants;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/file")
public class FileController extends ABaseController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Value("${project.folder:./upload}")
    private String projectFolder;

    @Resource
    private FileChunkService fileChunkService;

    // ======================================================
    // 原接口：小文件整体上传（保留向下兼容）
    // ======================================================
    @RequestMapping("/upload")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO uploadFile(HttpServletRequest request,
                                 @RequestParam("file") MultipartFile file,
                                 String meetingNo) {
        TokenUserInfoDto tokenUserInfo = getTokenUserInfo(request);
        if (tokenUserInfo == null) {
            return getServerErrorResponseVO("用户未登录");
        }
        if (file.isEmpty()) {
            return getServerErrorResponseVO("文件不能为空");
        }
        try {
            String fileId = StringTools.getUserRandomId();
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
            String dirPath = projectFolder + "/file/" + datePath;
            File dir = new File(dirPath);
            if (!dir.exists()) dir.mkdirs();

            File dest = new File(dir, fileId + ext);
            file.transferTo(dest);

            String contentType = file.getContentType();
            long fileSize = file.getSize();
            int fileType = getFileType(contentType, ext);
            String filePath = "/file/" + datePath + "/" + fileId + ext;

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("fileId", fileId);
            result.put("fileName", originalName);
            result.put("fileSize", fileSize);
            result.put("fileType", fileType);
            result.put("filePath", filePath);
            result.put("contentType", contentType);

            // 写入 Redis 文件索引（供秒传查询）
            try {
                String fileHash = DigestUtils.md5Hex(new FileInputStream(dest));
                Map<String, Object> hashInfo = new LinkedHashMap<>(result);
                redisUtils.setex(Contants.FILE_HASH + fileHash, hashInfo, 30L * 24 * 60 * 60);
                result.put("fileHash", fileHash);
            } catch (Exception e) {
                logger.warn("整体上传后计算 hash 索引失败（不影响上传结果）: {}", e.getMessage());
            }

            logger.info("文件上传成功: fileId={}, name={}, size={}", fileId, originalName, fileSize);
            return getSuccessResponseVO(result);
        } catch (Exception e) {
            logger.error("文件上传失败", e);
            return getServerErrorResponseVO("文件上传失败: " + e.getMessage());
        }
    }

    // ======================================================
    // 原接口：文件下载（支持 Range 断点续传）
    // ======================================================
    @RequestMapping("/download/{fileId}")
    @GlobalInterceptor(checkLogin = true)
    public void downloadFile(HttpServletRequest request,
                             HttpServletResponse response,
                             @PathVariable String fileId,
                             @RequestParam(defaultValue = "") String filePath) {
        try {
            String fullPath = projectFolder + filePath;
            File file = new File(fullPath);

            if (!file.exists()) {
                response.setStatus(404);
                response.getWriter().write("文件不存在");
                return;
            }

            String fileName = file.getName();
            String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".")).toLowerCase() : "";
            String contentType = getContentTypeByExt(ext);
            response.setContentType(contentType);
            response.setHeader("Content-Disposition", "inline; filename=\"" +
                    new String(fileName.getBytes("UTF-8"), "ISO-8859-1") + "\"");
            response.setHeader("Content-Length", String.valueOf(file.length()));
            response.setHeader("Accept-Ranges", "bytes");

            String rangeHeader = request.getHeader("Range");
            long startByte = 0;
            long endByte = file.length() - 1;
            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String[] ranges = rangeHeader.substring(6).split("-");
                try {
                    startByte = Long.parseLong(ranges[0]);
                    if (ranges.length > 1 && !ranges[1].isEmpty()) {
                        endByte = Long.parseLong(ranges[1]);
                    }
                } catch (NumberFormatException ignored) {
                    startByte = 0;
                    endByte = file.length() - 1;
                }
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                response.setHeader("Content-Range", "bytes " + startByte + "-" + endByte + "/" + file.length());
                response.setHeader("Content-Length", String.valueOf(endByte - startByte + 1));
            }

            try (RandomAccessFile raf = new RandomAccessFile(file, "r");
                 OutputStream os = response.getOutputStream()) {
                raf.seek(startByte);
                byte[] buffer = new byte[8192];
                long remaining = endByte - startByte + 1;
                int len;
                while (remaining > 0 && (len = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                    os.write(buffer, 0, len);
                    remaining -= len;
                }
                os.flush();
            }
        } catch (Exception e) {
            logger.error("文件下载失败: fileId={}", fileId, e);
        }
    }

    // ======================================================
    // 分片上传接口（委托给 FileChunkService）
    // ======================================================

    /** 预检：秒传 / 断点续传 / 全新 */
    @PostMapping("/upload/check")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO checkUpload(HttpServletRequest request,
                                  @RequestParam String fileHash,
                                  @RequestParam String fileName,
                                  @RequestParam long fileSize,
                                  @RequestParam(required = false, defaultValue = "0") long chunkSize,
                                  @RequestParam(required = false) String meetingNo) {
        TokenUserInfoDto userInfo = getTokenUserInfo(request);
        if (userInfo == null) return getServerErrorResponseVO("用户未登录");
        try {
            return fileChunkService.checkUpload(userInfo, fileHash, fileName, fileSize, chunkSize, meetingNo);
        } catch (Exception e) {
            logger.error("分片上传预检失败", e);
            return getServerErrorResponseVO(e.getMessage());
        }
    }

    /** 上传单个分片 */
    @PostMapping("/upload/chunk")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO uploadChunk(HttpServletRequest request,
                                  @RequestParam("file") MultipartFile file,
                                  @RequestParam String uploadId,
                                  @RequestParam int chunkIndex,
                                  @RequestParam(required = false) String chunkHash) {
        TokenUserInfoDto userInfo = getTokenUserInfo(request);
        if (userInfo == null) return getServerErrorResponseVO("用户未登录");
        if (file.isEmpty()) return getServerErrorResponseVO("分片不能为空");
        try {
            return fileChunkService.uploadChunk(userInfo, file.getBytes(), uploadId, chunkIndex, chunkHash);
        } catch (Exception e) {
            logger.error("分片上传失败 uploadId={}, chunkIndex={}", uploadId, chunkIndex, e);
            return getServerErrorResponseVO(e.getMessage());
        }
    }

    /** 合并分片 */
    @PostMapping("/upload/merge")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO mergeChunks(HttpServletRequest request,
                                  @RequestParam String uploadId,
                                  @RequestParam String fileHash,
                                  @RequestParam String fileName,
                                  @RequestParam int totalChunks,
                                  @RequestParam(required = false) String meetingNo) {
        TokenUserInfoDto userInfo = getTokenUserInfo(request);
        if (userInfo == null) return getServerErrorResponseVO("用户未登录");
        try {
            return fileChunkService.mergeChunks(userInfo, uploadId, fileHash, fileName, totalChunks, meetingNo);
        } catch (Exception e) {
            logger.error("分片合并失败 uploadId={}", uploadId, e);
            return getServerErrorResponseVO(e.getMessage());
        }
    }

    /** 取消上传 */
    @DeleteMapping("/upload/{uploadId}")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO cancelUpload(HttpServletRequest request,
                                   @PathVariable String uploadId) {
        TokenUserInfoDto userInfo = getTokenUserInfo(request);
        if (userInfo == null) return getServerErrorResponseVO("用户未登录");
        try {
            return fileChunkService.cancelUpload(userInfo, uploadId);
        } catch (Exception e) {
            logger.error("取消上传失败 uploadId={}", uploadId, e);
            return getServerErrorResponseVO(e.getMessage());
        }
    }

    /** 定时清理过期分片（每天凌晨 2 点） */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupStaleChunks() {
        fileChunkService.cleanupStaleChunks();
    }

    // ==================== 工具方法 ====================

    private int getFileType(String contentType, String ext) {
        String lowerExt = ext.toLowerCase();
        if (lowerExt.matches("\\.(jpg|jpeg|png|gif|bmp|webp|svg|ico)")) return 1;
        if (lowerExt.matches("\\.(mp4|avi|mov|wmv|flv|mkv|webm)")) return 2;
        if (lowerExt.matches("\\.(mp3|wav|aac|ogg|flac|wma)")) return 3;
        if (lowerExt.matches("\\.(doc|docx|xls|xlsx|ppt|pptx|pdf|txt)")) return 4;
        return 5;
    }

    private String getContentTypeByExt(String ext) {
        Map<String, String> map = new HashMap<>();
        map.put(".jpg", "image/jpeg"); map.put(".jpeg", "image/jpeg");
        map.put(".png", "image/png"); map.put(".gif", "image/gif");
        map.put(".webp", "image/webp"); map.put(".svg", "image/svg+xml");
        map.put(".mp4", "video/mp4"); map.put(".webm", "video/webm");
        map.put(".mp3", "audio/mpeg"); map.put(".wav", "audio/wav");
        map.put(".pdf", "application/pdf"); map.put(".txt", "text/plain");
        return map.getOrDefault(ext, "application/octet-stream");
    }

    // ======================================================
    // 静态文件访问（供前端 <img src="/file/2026/06/13/xxx.png"> 直接加载）
    // 精确匹配 /file/yyyy/MM/dd/fileName 路径
    // ======================================================
    @GetMapping("/{year:\\d{4}}/{month:\\d{2}}/{day:\\d{2}}/{fileName}")
    public void staticFile(@PathVariable String year,
                           @PathVariable String month,
                           @PathVariable String day,
                           @PathVariable String fileName,
                           HttpServletRequest request,
                           HttpServletResponse response) {
        logger.info("[文件访问] 收到请求: /file/{}/{}/{}/{}", year, month, day, fileName);
        try {
            String relativePath = year + "/" + month + "/" + day + "/" + fileName;
            File baseDir = new File(projectFolder, "file");
            File targetFile = new File(baseDir, relativePath);

            if (!targetFile.exists() || !targetFile.isFile()) {
                logger.warn("[文件访问] 找不到文件: {}", targetFile.getAbsolutePath());
                response.setStatus(404);
                return;
            }

            String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".")).toLowerCase() : "";
            response.setContentType(getContentTypeByExt(ext));
            response.setHeader("Accept-Ranges", "bytes");

            // HTTP 缓存
            long lastModified = targetFile.lastModified();
            long clientTime = request.getDateHeader("If-Modified-Since");
            if (clientTime >= 0 && clientTime >= lastModified / 1000 * 1000) {
                response.setStatus(304);
                return;
            }
            response.setDateHeader("Last-Modified", lastModified);
            response.setHeader("Cache-Control", "public, max-age=86400");

            // Range 断点续传
            long fileLength = targetFile.length();
            long start = 0, end = fileLength - 1;
            String range = request.getHeader("Range");
            if (range != null && range.startsWith("bytes=")) {
                String[] parts = range.substring(6).split("-");
                try {
                    if (parts.length > 0 && !parts[0].isEmpty()) start = Long.parseLong(parts[0]);
                    if (parts.length > 1 && !parts[1].isEmpty()) end = Long.parseLong(parts[1]);
                } catch (NumberFormatException ignored) {}
                if (start < 0) start = 0;
                if (end >= fileLength) end = fileLength - 1;
                if (start > end) { response.setStatus(416); return; }
                response.setStatus(206);
            }
            long contentLength = end - start + 1;
            response.setHeader("Content-Length", String.valueOf(contentLength));
            if (start > 0 || end < fileLength - 1) {
                response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
            }

            // 流式输出
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(targetFile))) {
                if (start > 0) bis.skip(start);
                byte[] buffer = new byte[8192];
                long remain = contentLength;
                int read;
                while (remain > 0 && (read = bis.read(buffer, 0, (int) Math.min(buffer.length, remain))) > 0) {
                    response.getOutputStream().write(buffer, 0, read);
                    remain -= read;
                }
                response.getOutputStream().flush();
            }
        } catch (Exception e) {
            logger.error("[文件访问] 异常: {}", e.getMessage());
            if (!response.isCommitted()) {
                response.setStatus(500);
            }
        }
    }
}