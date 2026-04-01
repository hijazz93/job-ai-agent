package com.hjz.jobaiagent.app;

import cn.hutool.core.lang.UUID;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class JobAppTest {

    @Resource
    private JobApp jobApp;

    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好，我是嘉章";
        String answer = jobApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第二轮
        message = "我想找Agent开发工作";
        answer = jobApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第三轮
        message = "我要找什么工作？刚跟你说过，帮我回忆一下";
        answer = jobApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithReport() {
        String chatId = java.util.UUID.randomUUID().toString();
        String message = "你好，我是嘉章。我想找Agent开发的工作岗位，但我不知道怎么做";
        JobApp.JobReport jobReport = jobApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(jobReport);
    }

    @Test
    void doChatWithRag() {
        String chatId = java.util.UUID.randomUUID().toString();
        String message = "软件工程校招的岗位方向众多，应届生该怎么选择适合自己的岗位？";
        String answer = jobApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
    }
}
