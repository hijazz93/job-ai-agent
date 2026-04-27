package com.hjz.jobaiagent.repository;

import com.hjz.jobaiagent.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);
    void deleteByUserIdAndId(Long userId, String id);
}
