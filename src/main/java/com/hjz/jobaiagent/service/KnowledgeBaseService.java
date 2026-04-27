package com.hjz.jobaiagent.service;

import com.hjz.jobaiagent.entity.KnowledgeDocument;
import com.hjz.jobaiagent.rag.MyKeywordEnricher;
import com.hjz.jobaiagent.rag.MyTokenTextSplitter;
import com.hjz.jobaiagent.repository.KnowledgeDocumentRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KnowledgeBaseService {

    @Resource
    private KnowledgeDocumentRepository repository;

    @Resource
    private VectorStore jobAppVectorStore;

    @Resource
    private MyKeywordEnricher myKeywordEnricher;

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    private final List<String> indexedDocIds = new ArrayList<>();
    private static final String DOC_ID_PREFIX = "kb_";

    /** 本地文档目录路径（相对于项目根目录） */
    private Path documentDir;

    @PostConstruct
    public void init() {
        documentDir = Path.of(System.getProperty("user.dir"), "src", "main", "resources", "document");
        try {
            Files.createDirectories(documentDir);
            log.info("知识库文档目录: {}", documentDir.toAbsolutePath().normalize());
        } catch (IOException e) {
            log.warn("无法创建文档目录: {}", documentDir, e);
        }
    }

    // ========== 文件系统与数据库同步 ==========

    /**
     * 同步本地文件系统与数据库，然后返回分页列表
     */
    @Transactional
    public Page<KnowledgeDocument> syncAndList(int page, int size, String keyword) {
        syncWithFileSystem();
        PageRequest pageRequest = PageRequest.of(page, size);
        if (keyword != null && !keyword.isBlank()) {
            return repository.findByTitleContainingOrDescriptionContaining(keyword, keyword, pageRequest);
        }
        return repository.findAll(pageRequest);
    }

    /**
     * 扫描本地文档目录，与数据库自动同步：
     * - 本地存在但数据库没有 → 自动录入
     * - 数据库存在但本地已删除 → 自动删除
     * - 本地和数据库都存在 → 更新内容（以本地文件为准）
     */
    private void syncWithFileSystem() {
        // 1. 扫描所有本地 .md 文件
        Map<String, Path> localFiles = new HashMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(documentDir, "*.md")) {
            for (Path file : stream) {
                String title = filenameToTitle(file.getFileName().toString());
                localFiles.put(title, file);
            }
        } catch (IOException e) {
            log.error("扫描文档目录失败: {}", documentDir, e);
            return;
        }

        // 2. 获取所有 DB 记录（按 title 索引）
        List<KnowledgeDocument> allDocs = repository.findAll();
        Map<String, KnowledgeDocument> dbByTitle = new HashMap<>();
        for (KnowledgeDocument doc : allDocs) {
            dbByTitle.put(doc.getTitle(), doc);
        }

        // 3. 处理新增 & 更新：文件存在但 DB 没有 → 录入；内容变更 → 更新
        for (Map.Entry<String, Path> entry : localFiles.entrySet()) {
            String title = entry.getKey();
            Path file = entry.getValue();
            try {
                String fileContent = Files.readString(file);
                KnowledgeDocument existing = dbByTitle.get(title);
                if (existing == null) {
                    // 新增：文件存在但 DB 无记录 → 创建
                    KnowledgeDocument doc = new KnowledgeDocument();
                    doc.setTitle(title);
                    doc.setContent(fileContent);
                    doc.setStatus("");
                    doc.setEnabled(true);
                    repository.save(doc);
                    log.info("知识库自动录入: {}", title);
                } else if (!fileContent.equals(existing.getContent())) {
                    // 更新：文件内容已变更 → 同步到 DB
                    existing.setContent(fileContent);
                    repository.save(existing);
                    log.info("知识库内容同步: {}", title);
                }
            } catch (IOException e) {
                log.error("读取文件失败: {}", file, e);
            }
        }

        // 4. 处理删除：DB 有但本地文件已不存在 → 删除 DB 记录
        for (KnowledgeDocument doc : allDocs) {
            if (!localFiles.containsKey(doc.getTitle())) {
                repository.delete(doc);
                log.info("知识库自动删除（文件已移除）: {}", doc.getTitle());
            }
        }
    }

    // ========== 单文档 CRUD（同时操作文件和数据库） ==========

    public Page<KnowledgeDocument> listDocuments(int page, int size, String keyword) {
        PageRequest pageRequest = PageRequest.of(page, size);
        if (keyword != null && !keyword.isBlank()) {
            return repository.findByTitleContainingOrDescriptionContaining(keyword, keyword, pageRequest);
        }
        return repository.findAll(pageRequest);
    }

    public KnowledgeDocument getDocument(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("知识库条目不存在"));
    }

    @Transactional
    public KnowledgeDocument createDocument(String title, String content, String description, String status) {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTitle(title);
        doc.setContent(content);
        doc.setDescription(description);
        doc.setStatus(status != null ? status : "");
        doc.setEnabled(true);
        doc = repository.save(doc);

        // 同步写入本地文件
        writeDocumentFile(title, content);
        return doc;
    }

    @Transactional
    public KnowledgeDocument updateDocument(Long id, String title, String content, String description,
                                             String status, Boolean enabled) {
        KnowledgeDocument doc = getDocument(id);
        String oldTitle = doc.getTitle();

        if (title != null) doc.setTitle(title);
        if (content != null) doc.setContent(content);
        if (description != null) doc.setDescription(description);
        if (status != null) doc.setStatus(status);
        if (enabled != null) doc.setEnabled(enabled);
        doc = repository.save(doc);

        // 同步更新本地文件
        String effectiveTitle = title != null ? title : oldTitle;
        if (title != null && !title.equals(oldTitle)) {
            deleteDocumentFile(oldTitle); // 标题变了 → 删除旧文件
        }
        writeDocumentFile(effectiveTitle, doc.getContent());
        return doc;
    }

    @Transactional
    public void deleteDocument(Long id) {
        KnowledgeDocument doc = getDocument(id);
        repository.deleteById(id);
        deleteDocumentFile(doc.getTitle());
    }

    // ========== 向量索引重建 ==========

    @Transactional
    public void rebuildVectorStore() {
        if (!indexedDocIds.isEmpty()) {
            jobAppVectorStore.delete(indexedDocIds);
            indexedDocIds.clear();
        }

        List<KnowledgeDocument> kbDocs = repository.findByEnabledTrue();
        if (kbDocs.isEmpty()) return;

        List<Document> documents = kbDocs.stream()
                .map(doc -> Document.builder()
                        .id(DOC_ID_PREFIX + doc.getId())
                        .text(doc.getContent() != null ? doc.getContent() : "")
                        .metadata(Map.of(
                                "filename", doc.getTitle() != null ? doc.getTitle() : "",
                                "status", doc.getStatus() != null ? doc.getStatus() : ""
                        ))
                        .build())
                .collect(Collectors.toList());

        List<Document> splitDocs = myTokenTextSplitter.splitDocuments(documents);
        List<Document> enriched = myKeywordEnricher.enrichDocuments(splitDocs);
        jobAppVectorStore.add(enriched);
        enriched.forEach(doc -> indexedDocIds.add(doc.getId()));
    }

    // ========== 文件操作辅助方法 ==========

    private void writeDocumentFile(String title, String content) {
        if (title == null || content == null) return;
        Path file = documentDir.resolve(titleToFilename(title));
        try {
            Files.writeString(file, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("知识库文件已写入: {}", file.getFileName());
        } catch (IOException e) {
            throw new RuntimeException("写入知识库文件失败: " + file, e);
        }
    }

    private void deleteDocumentFile(String title) {
        if (title == null) return;
        Path file = documentDir.resolve(titleToFilename(title));
        try {
            boolean deleted = Files.deleteIfExists(file);
            if (deleted) {
                log.info("知识库文件已删除: {}", file.getFileName());
            }
        } catch (IOException e) {
            log.warn("删除知识库文件失败: {}", file, e);
        }
    }

    /** 文件名（不含 .md）→ 标题 */
    private String filenameToTitle(String filename) {
        if (filename.toLowerCase(Locale.ROOT).endsWith(".md")) {
            return filename.substring(0, filename.length() - 3);
        }
        return filename;
    }

    /** 标题 → 合法文件名（替换非法字符） */
    private String titleToFilename(String title) {
        return title.replaceAll("[/\\\\:*?\"<>|]", "_") + ".md";
    }
}
