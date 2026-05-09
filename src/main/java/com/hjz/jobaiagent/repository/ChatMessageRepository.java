package com.hjz.jobaiagent.repository;

import com.hjz.jobaiagent.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    @Modifying
    @Query("DELETE FROM ChatMessage m WHERE m.session.id = :sessionId")
    void deleteBySessionId(String sessionId);
}
