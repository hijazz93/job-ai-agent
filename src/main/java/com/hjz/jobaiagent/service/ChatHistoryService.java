package com.hjz.jobaiagent.service;

import com.hjz.jobaiagent.entity.ChatMessage;
import com.hjz.jobaiagent.entity.ChatSession;
import com.hjz.jobaiagent.entity.User;
import com.hjz.jobaiagent.repository.ChatMessageRepository;
import com.hjz.jobaiagent.repository.ChatSessionRepository;
import com.hjz.jobaiagent.repository.SessionFileContextRepository;
import com.hjz.jobaiagent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;

@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SessionFileContextRepository sessionFileContextRepository;

    private final Object sessionCreationLock = new Object();

    @Transactional
    public ChatSession createSession(String sessionId, Long userId, String title) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        synchronized (sessionCreationLock) {
            return chatSessionRepository.findById(sessionId)
                    .orElseGet(() -> {
                        ChatSession session = new ChatSession();
                        session.setId(sessionId);
                        session.setUser(user);
                        session.setTitle(title != null ? title : "新对话");
                        try {
                            return chatSessionRepository.save(session);
                        } catch (DataIntegrityViolationException e) {
                            // 另一个线程抢先创建了此会话，直接读取即可
                            return chatSessionRepository.findById(sessionId)
                                    .orElseThrow(() -> new RuntimeException("创建会话失败"));
                        }
                    });
        }
    }

    public List<ChatSession> getUserSessions(Long userId) {
        return chatSessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    @Transactional
    public void updateSessionTitle(String sessionId, String title) {
        chatSessionRepository.findById(sessionId).ifPresent(session -> {
            session.setTitle(title);
            chatSessionRepository.save(session);
        });
    }

    @Transactional
    public void deleteSession(String sessionId, Long userId) {
        // 必须先删除关联消息，再删除会话，否则违反外键约束
        chatMessageRepository.deleteBySessionId(sessionId);
        sessionFileContextRepository.deleteBySessionId(sessionId);
        chatSessionRepository.deleteByUserIdAndId(userId, sessionId);
    }

    public List<ChatMessage> getSessionMessages(String sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    @Transactional
    public ChatMessage saveMessage(String sessionId, Long userId, String role, String content) {
        ChatSession session = chatSessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            synchronized (sessionCreationLock) {
                session = chatSessionRepository.findById(sessionId)
                        .orElseGet(() -> {
                            ChatSession newSession = new ChatSession();
                            newSession.setId(sessionId);
                            newSession.setTitle("新对话");
                            if (userId != null) {
                                User user = userRepository.findById(userId)
                                        .orElseThrow(() -> new RuntimeException("用户不存在"));
                                newSession.setUser(user);
                            }
                            try {
                                return chatSessionRepository.save(newSession);
                            } catch (DataIntegrityViolationException e) {
                                return chatSessionRepository.findById(sessionId)
                                        .orElseThrow(() -> new RuntimeException("创建会话失败"));
                            }
                        });
            }
        }

        // 会话标题为默认"新对话"时，用首条用户消息自动更新
        if ("新对话".equals(session.getTitle()) && "user".equals(role) && content != null && !content.isEmpty()) {
            String newTitle = content.length() > 30 ? content.substring(0, 30) : content;
            session.setTitle(newTitle);
            chatSessionRepository.save(session);
        }

        ChatMessage message = new ChatMessage();
        message.setSession(session);
        message.setRole(role);
        message.setContent(content);
        return chatMessageRepository.save(message);
    }
}
