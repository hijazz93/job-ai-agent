package com.hjz.jobaiagent.repository;

import com.hjz.jobaiagent.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);

    @Modifying
    @Query("DELETE FROM ChatSession s WHERE s.user.id = :userId AND s.id = :id")
    void deleteByUserIdAndId(Long userId, String id);
}
