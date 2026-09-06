package com.meetchat.Controller;

import com.meetchat.annotation.GlobalInterceptor;
import com.meetchat.entity.Dto.TokenUserInfoDto;
import com.meetchat.entity.vo.ResponseVO;
import com.meetchat.utils.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 头像上传/下载控制器
 *
 * - POST /api/avatar/upload   上传机器人/用户头像，存到 projectFolder/avatar/yyyy/MM/dd/fileId.ext
 * - GET  /api/avatar/download/{fileId}  头像下载（支持 Range）
 * - GET  /api/avatar/{year}/{month}/{day}/{fileName}  静态文件直接访问（供 <img src>）
 *
 * 返回字段约定（与前端 file.js uploadAvatar 期望一致）：
 *   {
 *     fileId:   "随机ID",
 *     fileName: "原始文件名",
 *     fileSize: 字节数,
 *     filePath: "/avatar/yyyy/MM/dd/fileId.ext",
 *     fileType: 1,
 *     contentType: "image/png"
 *   }
 */
@RestController
@RequestMapping("/avatar")
public class AvatarController extends ABaseController {

    private static final Logger logger = LoggerFactory.getLogger(AvatarController.class);

    @Value("${project.folder:./upload}")
    private String projectFolder;

    // ======================================================
    // 头像上传（multipart/form-data，字段: file + userId）
    // ======================================================
    @RequestMapping("/upload")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO uploadAvatar(HttpServletRequest request,
                                   @RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "userId", required = false) String userId) {
        TokenUserInfoDto tokenUserInfo = getTokenUserInfo(request);
        if (tokenUserInfo == null) {
            return getServerErrorResponseVO("用户未登录");
        }
        if (file.isEmpty()) {
            return getServerErrorResponseVO("头像文件不能为空");
        }

        // 限制头像大小（2MB）
        long maxSize = 2L * 1024 * 1024;
        if (file.getSize() > maxSize) {
            return getServerErrorResponseVO("头像大小不能超过 2MB");
        }

        try {
            String fileId = StringTools.getUserRandomId();
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
            }
            // 兜底：必须是图片后缀
            if (ext.isEmpty() || !ext.matches("\\.(jpg|jpeg|png|gif|bmp|webp|svg|ico)")) {
                ext = ".png";
            }

            String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
            String dirPath = projectFolder + "/avatar/" + datePath;
            File dir = new File(dirPath);
            if (!dir.exists()) dir.mkdirs();

            File dest = new File(dir, fileId + ext);
            file.transferTo(dest);

            String contentType = file.getContentType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = getContentTypeByExt(ext);
            }

            // 相对路径（与 FileController 风格保持一致，不带 projectFolder 前缀）
            String filePath = "/avatar/" + datePath + "/" + fileId + ext;

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("fileId", fileId);
            result.put("fileName", originalName);
            result.put("fileSize", file.getSize());
            result.put("fileType", 1); // 1 = 图片
            result.put("filePath", filePath);
            result.put("contentType", contentType);

            logger.info("头像上传成功: fileId={}, userId={}, name={}, size={}",
                    fileId, userId, originalName, file.getSize());
            return getSuccessResponseVO(result);
        } catch (Exception e) {
            logger.error("头像上传失败", e);
            return getServerErrorResponseVO("头像上传失败: " + e.getMessage());
        }
    }

    // ======================================================
    // 头像下载（支持 Range 断点续传）
    // ======================================================
    @RequestMapping("/download/{fileId}")
    @GlobalInterceptor(checkLogin = true)
    public void downloadAvatar(HttpServletRequest request,
                               HttpServletResponse response,
                               @PathVariable String fileId,
                               @RequestParam(defaultValue = "") String filePath) {
        try {
            String fullPath;
            if (filePath != null && !filePath.isEmpty()) {
                fullPath = projectFolder + filePath;
            } else {
                fullPath = projectFolder + "/avatar/" + fileId;
            }
            File file = new File(fullPath);

            if (!file.exists()) {
                response.setStatus(404);
                response.getWriter().write("头像不存在");
                return;
            }

            String fileName = file.getName();
            String ext = fileName.contains(".")
                    ? fileName.substring(fileName.lastIndexOf(".")).toLowerCase() : "";
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

            try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r");
                 OutputStream os = response.getOutputStream()) {
                raf.seek(startByte);
                byte[] buffer = new byte[8192];
                long remaining = endByte - startByte + 1;
                int len;
                while (remaining > 0
                        && (len = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                    os.write(buffer, 0, len);
                    remaining -= len;
                }
                os.flush();
            }
        } catch (Exception e) {
            logger.error("头像下载失败: fileId={}", fileId, e);
        }
    }

    // ======================================================
    // 静态文件访问（供前端 <img src="/avatar/yyyy/MM/dd/xxx.png"> 直接加载）
    // ======================================================
    @GetMapping("/{year:\\d{4}}/{month:\\d{2}}/{day:\\d{2}}/{fileName}")
    public void staticAvatar(@PathVariable String year,
                             @PathVariable String month,
                             @PathVariable String day,
                             @PathVariable String fileName,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        logger.info("[头像访问] 收到请求: /avatar/{}/{}/{}/{}", year, month, day, fileName);
        try {
            String relativePath = year + "/" + month + "/" + day + "/" + fileName;
            File baseDir = new File(projectFolder, "avatar");
            File targetFile = new File(baseDir, relativePath);

            if (!targetFile.exists() || !targetFile.isFile()) {
                logger.warn("[头像访问] 找不到文件: {}", targetFile.getAbsolutePath());
                response.setStatus(404);
                return;
            }

            String ext = fileName.contains(".")
                    ? fileName.substring(fileName.lastIndexOf(".")).toLowerCase() : "";
            response.setContentType(getContentTypeByExt(ext));
            response.setHeader("Accept-Ranges", "bytes");

            long lastModified = targetFile.lastModified();
            long clientTime = request.getDateHeader("If-Modified-Since");
            if (clientTime >= 0 && clientTime >= lastModified / 1000 * 1000) {
                response.setStatus(304);
                return;
            }
            response.setDateHeader("Last-Modified", lastModified);
            response.setHeader("Cache-Control", "public, max-age=86400");

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

            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(targetFile))) {
                if (start > 0) bis.skip(start);
                byte[] buffer = new byte[8192];
                long remain = contentLength;
                int read;
                while (remain > 0
                        && (read = bis.read(buffer, 0, (int) Math.min(buffer.length, remain))) > 0) {
                    response.getOutputStream().write(buffer, 0, read);
                    remain -= read;
                }
                response.getOutputStream().flush();
            }
        } catch (Exception e) {
            logger.error("[头像访问] 异常: {}", e.getMessage());
            if (!response.isCommitted()) {
                response.setStatus(500);
            }
        }
    }

    // ==================== 工具方法 ====================

    private String getContentTypeByExt(String ext) {
        Map<String, String> map = new HashMap<>();
        map.put(".jpg", "image/jpeg");
        map.put(".jpeg", "image/jpeg");
        map.put(".png", "image/png");
        map.put(".gif", "image/gif");
        map.put(".webp", "image/webp");
        map.put(".svg", "image/svg+xml");
        map.put(".bmp", "image/bmp");
        map.put(".ico", "image/x-icon");
        return map.getOrDefault(ext, "application/octet-stream");
    }
}