package com.hjz.jobaiagent.repository;

import com.hjz.jobaiagent.entity.SessionFileContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SessionFileContextRepository extends JpaRepository<SessionFileContext, Long> {

    List<SessionFileContext> findBySessionIdOrderByUploadedAtAsc(String sessionId);

    @Modifying
    @Query("DELETE FROM SessionFileContext f WHERE f.sessionId = :sessionId")
    void deleteBySessionId(String sessionId);

    @Modifying
    @Query("DELETE FROM SessionFileContext f WHERE f.sessionId = :sessionId AND f.id = :id")
    void deleteBySessionIdAndId(String sessionId, Long id);

    boolean existsBySessionIdAndUniqueFileName(String sessionId, String uniqueFileName);
}
