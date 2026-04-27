package com.hjz.jobaiagent.controller;

import com.hjz.jobaiagent.constant.FileConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import cn.hutool.core.io.FileUtil;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文档上传控制器 - 提供文档上传至知识库的功能
 */
@RestController
@RequestMapping("/documents")
@Slf4j
public class DocumentUploadController {

    @Value("${document.upload.path:tmp}")
    private String uploadPath;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        "doc", "docx", "pdf", "txt", "md", "markdown",
        "xls", "xlsx", "ppt", "pptx",
        "csv", "json", "xml", "html",
        "jpg", "jpeg", "png", "gif"
    );

    private static final Map<String, DocumentInfo> uploadHistory = new ConcurrentHashMap<>();

    static {
        uploadHistory.put("demo-001", new DocumentInfo(
            "demo-001",
            "示例文档.pdf",
            "pdf",
            1024 * 1024,
            new Date(),
            "/api/documents/demo-001"
        ));
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "上传文件不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "文件名不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            String fileExtension = getFileExtension(originalFilename).toLowerCase();
            
            if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
                response.put("success", false);
                response.put("message", "不支持的文件格式：" + fileExtension + "，支持的格式：" + ALLOWED_EXTENSIONS);
                return ResponseEntity.badRequest().body(response);
            }

            if (file.getSize() > MAX_FILE_SIZE) {
                response.put("success", false);
                response.put("message", "文件大小超过限制（最大10MB），当前文件大小：" + formatFileSize(file.getSize()));
                return ResponseEntity.badRequest().body(response);
            }

            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String uniqueFileName = generateUniqueFileName(originalFilename);
            Path targetPath = uploadDir.resolve(uniqueFileName);

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String documentId = UUID.randomUUID().toString().substring(0, 8);

            DocumentInfo docInfo = new DocumentInfo(
                documentId,
                originalFilename,
                fileExtension,
                file.getSize(),
                new Date(),
                "/api/documents/" + uniqueFileName
            );
            docInfo.setUniqueFileName(uniqueFileName);
            docInfo.setFullPath(targetPath.toAbsolutePath().toString());

            uploadHistory.put(documentId, docInfo);

            log.info("文档上传成功：{} -> {}", originalFilename, uniqueFileName);

            response.put("success", true);
            response.put("message", "文档上传成功");
            response.put("data", docInfo);
            
            return ResponseEntity.ok().body(response);

        } catch (IOException e) {
            log.error("文档上传失败", e);
            response.put("success", false);
            response.put("message", "文档上传失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getUploadHistory() {
        Map<String, Object> response = new HashMap<>();
        
        List<DocumentInfo> historyList = new ArrayList<>(uploadHistory.values());
        historyList.sort((a, b) -> b.getUploadTime().compareTo(a.getUploadTime()));
        
        response.put("success", true);
        response.put("data", historyList);
        response.put("total", historyList.size());
        
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/{fileName}")
    public ResponseEntity<byte[]> previewDocument(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get(uploadPath, fileName);
            
            if (!Files.exists(filePath)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "文件不存在");
                return ResponseEntity.notFound().build();
            }

            byte[] content = Files.readAllBytes(filePath);
            String contentType = determineContentType(fileName);

            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .header("Content-Disposition", "inline; filename=\"" + fileName + "\"")
                    .body(content);
                    
        } catch (IOException e) {
            log.error("预览文档失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable String documentId) {
        Map<String, Object> response = new HashMap<>();
        
        DocumentInfo docInfo = uploadHistory.get(documentId);
        if (docInfo == null) {
            response.put("success", false);
            response.put("message", "文档不存在");
            return ResponseEntity.notFound().build();
        }
        
        try {
            String fileName = extractFileNameFromUrl(docInfo.getUrl());
            Path filePath = Paths.get(uploadPath, fileName);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
            
            uploadHistory.remove(documentId);
            
            response.put("success", true);
            response.put("message", "文档删除成功");
            return ResponseEntity.ok().body(response);
            
        } catch (IOException e) {
            log.error("删除文档失败", e);
            response.put("success", false);
            response.put("message", "删除失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/parse/{fileName}")
    public ResponseEntity<Map<String, Object>> parseDocument(@PathVariable String fileName) {
        Map<String, Object> response = new HashMap<>();
        Path filePath = Paths.get(uploadPath, fileName);

        if (!Files.exists(filePath)) {
            response.put("success", false);
            response.put("message", "文件不存在: " + fileName);
            return ResponseEntity.badRequest().body(response);
        }

        try {
            String content = parseFileContent(filePath.toFile());
            response.put("success", true);
            response.put("data", Map.of(
                "fileName", fileName,
                "content", content,
                "charCount", content.length()
            ));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("文件解析失败: {}", fileName, e);
            response.put("success", false);
            response.put("message", "文件解析失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    private String parseFileContent(File file) throws IOException {
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".markdown")) {
            return FileUtil.readUtf8String(file);
        } else if (name.endsWith(".pdf")) {
            try (PDDocument doc = Loader.loadPDF(file)) {
                return new PDFTextStripper().getText(doc);
            }
        } else if (name.endsWith(".doc")) {
            try (FileInputStream fis = new FileInputStream(file);
                 HWPFDocument doc = new HWPFDocument(fis);
                 WordExtractor extractor = new WordExtractor(doc)) {
                return extractor.getText();
            }
        } else if (name.endsWith(".docx")) {
            try (FileInputStream fis = new FileInputStream(file);
                 XWPFDocument doc = new XWPFDocument(fis);
                 XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                return extractor.getText();
            }
        }
        throw new IOException("不支持的文件格式: " + name);
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    private String generateUniqueFileName(String originalFilename) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS");
        String timestamp = sdf.format(new Date());
        String randomStr = UUID.randomUUID().toString().substring(0, 6);
        String extension = getFileExtension(originalFilename);
        String baseName = originalFilename.contains(".") 
            ? originalFilename.substring(0, originalFilename.lastIndexOf('.'))
            : originalFilename;
        
        baseName = baseName.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "_");
        
        if (baseName.length() > 20) {
            baseName = baseName.substring(0, 20);
        }
        
        return baseName + "_" + timestamp + "_" + randomStr + "." + extension;
    }

    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        }
    }

    private String determineContentType(String fileName) {
        String ext = getFileExtension(fileName).toLowerCase();
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "txt", "md", "markdown" -> "text/plain; charset=utf-8";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            case "html" -> "text/html";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "csv" -> "text/csv";
            default -> "application/octet-stream";
        };
    }

    private String extractFileNameFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }
        int lastSlash = url.lastIndexOf('/');
        return lastSlash >= 0 ? url.substring(lastSlash + 1) : url;
    }

    public static class DocumentInfo {
        private String id;
        private String originalName;
        private String uniqueFileName;
        private String fullPath;
        private String fileType;
        private long fileSize;
        private Date uploadTime;
        private String url;

        public DocumentInfo() {}

        public DocumentInfo(String id, String originalName, String fileType,
                          long fileSize, Date uploadTime, String url) {
            this.id = id;
            this.originalName = originalName;
            this.fileType = fileType;
            this.fileSize = fileSize;
            this.uploadTime = uploadTime;
            this.url = url;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getOriginalName() { return originalName; }
        public void setOriginalName(String originalName) { this.originalName = originalName; }

        public String getUniqueFileName() { return uniqueFileName; }
        public void setUniqueFileName(String uniqueFileName) { this.uniqueFileName = uniqueFileName; }

        public String getFullPath() { return fullPath; }
        public void setFullPath(String fullPath) { this.fullPath = fullPath; }

        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }

        public long getFileSize() { return fileSize; }
        public void setFileSize(long fileSize) { this.fileSize = fileSize; }

        public Date getUploadTime() { return uploadTime; }
        public void setUploadTime(Date uploadTime) { this.uploadTime = uploadTime; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
}
