package com.hjz.jobaiagent.tools;

import cn.hutool.core.io.FileUtil;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 文件解析工具 — 从 txt、md、pdf、doc、docx 文件中提取文本内容
 */
public class FileParseTool {

    private final String documentUploadPath;
    private final Path userDir;
    private static final int MAX_OUTPUT_CHARS = 10000;
    private static final List<String> SUPPORTED_EXTENSIONS = List.of("txt", "md", "pdf", "doc", "docx");

    public FileParseTool(String documentUploadPath) {
        this.documentUploadPath = documentUploadPath;
        this.userDir = Path.of(System.getProperty("user.dir"));
    }

    @Tool(description = "Parse and extract text content from a file. Supports txt, md, pdf, doc, docx formats.")
    public String parseFile(@ToolParam(description = "File path or file name to parse") String filePath) {
        if (filePath.contains("..")) {
            return "错误：文件路径不能包含 '..'，不允许路径穿越。";
        }

        List<String> searchedPaths = new ArrayList<>();
        Path resolved = resolvePath(filePath, searchedPaths);
        if (resolved == null) {
            StringBuilder sb = new StringBuilder("文件未找到：").append(filePath).append("\n已搜索以下路径：");
            for (String p : searchedPaths) {
                sb.append("\n  - ").append(p);
            }
            return sb.toString();
        }

        File file = resolved.toFile();
        String name = file.getName().toLowerCase(Locale.ROOT);
        String content;
        try {
            if (name.endsWith(".txt") || name.endsWith(".md")) {
                content = FileUtil.readUtf8String(file);
            } else if (name.endsWith(".pdf")) {
                content = parsePdf(file);
            } else if (name.endsWith(".doc")) {
                content = parseDoc(file);
            } else if (name.endsWith(".docx")) {
                content = parseDocx(file);
            } else {
                return "不支持的文件格式：" + getExtension(name) + "。支持的格式：" + String.join(", ", SUPPORTED_EXTENSIONS);
            }
        } catch (Exception e) {
            return "文件解析失败：" + e.getMessage();
        }

        if (content == null || content.isBlank()) {
            return "文件存在但无内容：" + resolved.toAbsolutePath();
        }

        if (content.length() > MAX_OUTPUT_CHARS) {
            content = content.substring(0, MAX_OUTPUT_CHARS)
                    + "\n\n[内容已截断，原始总计 " + content.length() + " 字符]";
        }

        return content;
    }

    private Path resolvePath(String filePath, List<String> searchedPaths) {
        Path p = Path.of(filePath);
        if (p.isAbsolute()) {
            if (Files.exists(p)) return p;
            searchedPaths.add(p.toString());
            return null;
        }

        p = userDir.resolve(filePath);
        if (Files.exists(p)) return p;
        searchedPaths.add(p.toAbsolutePath().toString());

        String[] subdirs = {"tmp/file", "tmp/pdf", "tmp/download"};
        for (String subdir : subdirs) {
            p = userDir.resolve(subdir).resolve(filePath);
            if (Files.exists(p)) return p;
            searchedPaths.add(p.toAbsolutePath().toString());
        }

        p = userDir.resolve("tmp").resolve(filePath);
        if (Files.exists(p)) return p;
        searchedPaths.add(p.toAbsolutePath().toString());

        p = Path.of(documentUploadPath).resolve(filePath);
        if (Files.exists(p)) return p;
        searchedPaths.add(p.toAbsolutePath().toString());

        return null;
    }

    private String parsePdf(File file) throws IOException {
        try (PDDocument doc = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private String parseDoc(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             HWPFDocument doc = new HWPFDocument(fis)) {
            WordExtractor extractor = new WordExtractor(doc);
            return extractor.getText();
        }
    }

    private String parseDocx(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument doc = new XWPFDocument(fis)) {
            XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
            return extractor.getText();
        }
    }

    private String getExtension(String filename) {
        int i = filename.lastIndexOf('.');
        return i >= 0 ? filename.substring(i) : "";
    }
}
