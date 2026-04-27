package com.hjz.jobaiagent.chatmemory;

import com.hjz.jobaiagent.entity.ChatMessage;
import com.hjz.jobaiagent.entity.ChatSession;
import com.hjz.jobaiagent.repository.ChatMessageRepository;
import com.hjz.jobaiagent.repository.ChatSessionRepository;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于数据库的对话记忆实现，替代原有的 FileBasedChatMemory
 */
public class DatabaseChatMemory implements ChatMemory {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionRepository chatSessionRepository;

    public DatabaseChatMemory(ChatMessageRepository chatMessageRepository,
                              ChatSessionRepository chatSessionRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.chatSessionRepository = chatSessionRepository;
    }

    @Override
    @Transactional
    public void add(String conversationId, List<Message> messages) {
        // 消息已由前端 syncMessageToServer 持久化，此处避免重复保存
        ChatSession session = chatSessionRepository.findById(conversationId).orElse(null);
        if (session == null) {
            return;
        }
        session.setUpdatedAt(java.time.LocalDateTime.now());
        chatSessionRepository.save(session);
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<ChatMessage> messages = chatMessageRepository
                .findBySessionIdOrderByCreatedAtAsc(conversationId);

        return messages.stream()
                .skip(Math.max(0, messages.size() - lastN))
                .map(this::toSpringAiMessage)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void clear(String conversationId) {
        chatMessageRepository.deleteBySessionId(conversationId);
    }

    private Message toSpringAiMessage(ChatMessage chatMessage) {
        return switch (chatMessage.getRole()) {
            case "user" -> new UserMessage(chatMessage.getContent());
            case "assistant" -> new AssistantMessage(chatMessage.getContent());
            case "system" -> new SystemMessage(chatMessage.getContent());
            default -> new UserMessage(chatMessage.getContent());
        };
    }

}
