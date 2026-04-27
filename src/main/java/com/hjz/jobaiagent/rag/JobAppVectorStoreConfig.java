package com.hjz.jobaiagent.rag;


import com.hjz.jobaiagent.entity.KnowledgeDocument;
import com.hjz.jobaiagent.repository.KnowledgeDocumentRepository;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 就业助手应用向量数据库配置（初始化基于内存的向量数据库 Bean）
 */
@Configuration
public class JobAppVectorStoreConfig {

    @Resource
    private JobAppDocumentLoader jobAppDocumentLoader;

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    @Resource
    private MyKeywordEnricher myKeywordEnricher;

    @Autowired(required = false)
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Bean
    VectorStore jobAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel).build();
        // 加载文档
        List<Document> documentList = jobAppDocumentLoader.loadMarkdowns();

        // 从数据库加载已启用的知识库文档
        if (knowledgeDocumentRepository != null) {
            List<KnowledgeDocument> kbDocs = knowledgeDocumentRepository.findByEnabledTrue();
            for (KnowledgeDocument kbDoc : kbDocs) {
                documentList.add(Document.builder()
                        .id("kb_" + kbDoc.getId())
                        .text(kbDoc.getContent() != null ? kbDoc.getContent() : "")
                        .metadata(Map.of(
                                "filename", kbDoc.getTitle() != null ? kbDoc.getTitle() : "",
                                "status", kbDoc.getStatus() != null ? kbDoc.getStatus() : ""
                        ))
                        .build());
            }
        }

        // 分割文档
        List<Document> splitDocuments = myTokenTextSplitter.splitDocuments(documentList);
        // 自动补充关键词元信息
        List<Document> enrichedDocuments = myKeywordEnricher.enrichDocuments(splitDocuments);

        simpleVectorStore.add(enrichedDocuments);
        return simpleVectorStore;
    }
}