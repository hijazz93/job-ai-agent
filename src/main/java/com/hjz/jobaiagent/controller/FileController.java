package com.hjz.jobaiagent.controller;

import com.hjz.jobaiagent.constant.FileConstant;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件访问控制器 - 提供 PDF/TXT/图片 预览和下载
 */
@RestController
@RequestMapping("/files")
public class FileController {

    private static final String PDF_DIR = "/pdf";
    private static final String TXT_DIR = "/txt";
    private static final String IMAGE_DIR = "/images";
    private static final String DOWNLOAD_DIR = "/download";
    private static final String FILE_DIR = "/file";

    /**
     * 安全地解析文件路径，防止目录遍历攻击
     */
    private File resolveSafeFile(String subDir, String fileName) {
        if (fileName == null || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return null;
        }
        Path basePath = Paths.get(FileConstant.FILE_SAVE_DIR, subDir).normalize();
        Path filePath = basePath.resolve(fileName).normalize();
        if (!filePath.startsWith(basePath)) {
            return null;
        }
        return filePath.toFile();
    }

    // ==================== PDF 文件 ====================

    @GetMapping("/pdf/{fileName}")
    public ResponseEntity<Resource> previewPdf(@PathVariable String fileName) {
        File file = resolveSafeFile(PDF_DIR, fileName);
        if (file == null || !file.exists()) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @GetMapping("/pdf/{fileName}/download")
    public ResponseEntity<Resource> downloadPdf(@PathVariable String fileName) {
        File file = resolveSafeFile(PDF_DIR, fileName);
        if (file == null || !file.exists()) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    // ==================== TXT 文件 ====================

    @GetMapping("/txt/{fileName}")
    public ResponseEntity<Resource> previewTxt(@PathVariable String fileName) {
        File file = resolveSafeFile(TXT_DIR, fileName);
        if (file == null || !file.exists()) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(resource);
    }

    @GetMapping("/txt/{fileName}/download")
    public ResponseEntity<Resource> downloadTxt(@PathVariable String fileName) {
        File file = resolveSafeFile(TXT_DIR, fileName);
        if (file == null || !file.exists()) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                .body(resource);
    }

    // ==================== 图片文件 ====================

    private MediaType getImageMediaType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        } else if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        } else if (lower.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        } else if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    @GetMapping("/images/{fileName}")
    public ResponseEntity<Resource> previewImage(@PathVariable String fileName) {
        File file = resolveSafeFile(IMAGE_DIR, fileName);
        if (file == null || !file.exists()) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentType(getImageMediaType(fileName))
                .body(resource);
    }

    @GetMapping("/images/{fileName}/download")
    public ResponseEntity<Resource> downloadImage(@PathVariable String fileName) {
        File file = resolveSafeFile(IMAGE_DIR, fileName);
        if (file == null || !file.exists()) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(getImageMediaType(fileName))
                .body(resource);
    }

    // ==================== 下载文件 (downloadResource工具) ====================

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> previewDownloadFile(@PathVariable String fileName) {
        File file = resolveSafeFile(DOWNLOAD_DIR, fileName);
        if (file == null || !file.exists()) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource resource = new FileSystemResource(file);
        MediaType mediaType = getImageMediaType(fileName);
        if (fileName.toLowerCase().endsWith(".txt")) {
            mediaType = new MediaType("text", "plain", StandardCharsets.UTF_8);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentType(mediaType)
                .body(resource);
    }

    @GetMapping("/download/{fileName}/download")
    public ResponseEntity<Resource> downloadDownloadFile(@PathVariable String fileName) {
        File file = resolveSafeFile(DOWNLOAD_DIR, fileName);
        if (file == null || !file.exists()) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource resource = new FileSystemResource(file);
        MediaType mediaType = getImageMediaType(fileName);
        if (fileName.toLowerCase().endsWith(".txt")) {
            mediaType = new MediaType("text", "plain", StandardCharsets.UTF_8);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(mediaType)
                .body(resource);
    }

    // ==================== 通用文件 (writeFile工具 - /tmp/file/) ====================

    /**
     * 判断文件是否为文本类型
     */
    private boolean isTextFile(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".markdown") ||
               lower.endsWith(".json") || lower.endsWith(".csv") || lower.endsWith(".xml") ||
               lower.endsWith(".html") || lower.endsWith(".css") || lower.endsWith(".js") ||
               lower.endsWith(".py") || lower.endsWith(".java") || lower.endsWith(".log");
    }

    @GetMapping("/file/{fileName}")
    public ResponseEntity<Resource> previewFile(@PathVariable String fileName) {
        File file = resolveSafeFile(FILE_DIR, fileName);
        if (file == null || !file.exists()) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource resource = new FileSystemResource(file);
        MediaType mediaType;
        if (isTextFile(fileName)) {
            mediaType = new MediaType("text", "plain", StandardCharsets.UTF_8);
        } else {
            mediaType = getImageMediaType(fileName);
            if (mediaType.equals(MediaType.APPLICATION_OCTET_STREAM)) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentType(mediaType)
                .body(resource);
    }

    @GetMapping("/file/{fileName}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        File file = resolveSafeFile(FILE_DIR, fileName);
        if (file == null || !file.exists()) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource resource = new FileSystemResource(file);
        MediaType mediaType;
        if (isTextFile(fileName)) {
            mediaType = new MediaType("text", "plain", StandardCharsets.UTF_8);
        } else {
            mediaType = getImageMediaType(fileName);
            if (mediaType.equals(MediaType.APPLICATION_OCTET_STREAM)) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(mediaType)
                .body(resource);
    }
}
