package com.hjz.jobaiagent.repository;

import com.hjz.jobaiagent.entity.SessionFileContext;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionFileContextRepository extends JpaRepository<SessionFileContext, Long> {

    List<SessionFileContext> findBySessionIdOrderByUploadedAtAsc(String sessionId);

    void deleteBySessionId(String sessionId);

    void deleteBySessionIdAndId(String sessionId, Long id);

    boolean existsBySessionIdAndUniqueFileName(String sessionId, String uniqueFileName);
}
