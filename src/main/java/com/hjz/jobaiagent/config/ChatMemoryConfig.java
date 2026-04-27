package com.hjz.jobaiagent.config;

import com.hjz.jobaiagent.chatmemory.DatabaseChatMemory;
import com.hjz.jobaiagent.repository.ChatMessageRepository;
import com.hjz.jobaiagent.repository.ChatSessionRepository;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ChatMemoryConfig {

    @Bean
    @Primary
    public ChatMemory databaseChatMemory(ChatMessageRepository messageRepo,
                                         ChatSessionRepository sessionRepo) {
        return new DatabaseChatMemory(messageRepo, sessionRepo);
    }
}
