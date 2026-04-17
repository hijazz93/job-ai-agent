package com.hjz.jobaiagent.rag;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 就业助手应用文档加载器
 */
@Component
@Slf4j
public class JobAppDocumentLoader {

    private final ResourcePatternResolver resourcePatternResolver;

    public JobAppDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }


    /**
     * 加载多篇markdown文档
     * @return
     */
    public List<Document> loadMarkdowns() {
        List<Document> allDocuments = new ArrayList<>();
        try {
            // 多个 Markdown 文件的路径
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                String status = fileName.substring(fileName.length() - 11, fileName.length() - 5);  // 提取文档文件名倒数第11个字符到第数第5个字符，例：校招入门准备
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("filename", fileName)   // 文件名保存为额外信息
                        .withAdditionalMetadata("status", status)   // 标签保存为额外信息，例：校招入门准备

                        .build();
                MarkdownDocumentReader markdownDocumentReader  = new MarkdownDocumentReader(resource, config);
                allDocuments.addAll(markdownDocumentReader .get());
            }
        } catch (IOException e) {
            log.error("Markdown 文档加载失败", e);
        }
        return allDocuments;
    }
}
