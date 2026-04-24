package com.hjz.jobaiagent.controller;

import com.hjz.jobaiagent.constant.FileConstant;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;

/**
 * 文件访问控制器 - 提供 PDF/文件预览和下载
 */
@RestController
@RequestMapping("/files")
public class FileController {

    @GetMapping("/pdf/{fileName}")
    public ResponseEntity<Resource> previewPdf(@PathVariable String fileName) {
        String filePath = FileConstant.FILE_SAVE_DIR + "/pdf/" + fileName;
        File file = new File(filePath);

        if (!file.exists()) {
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
        String filePath = FileConstant.FILE_SAVE_DIR + "/pdf/" + fileName;
        File file = new File(filePath);

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        FileSystemResource resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}