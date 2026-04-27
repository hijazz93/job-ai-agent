package com.hjz.jobaiagent.repository;

import com.hjz.jobaiagent.entity.KnowledgeDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

    List<KnowledgeDocument> findByEnabledTrue();

    Page<KnowledgeDocument> findByTitleContainingOrDescriptionContaining(String title, String description, Pageable pageable);

    Page<KnowledgeDocument> findByTitleContaining(String keyword, Pageable pageable);
}
