package com.hjz.jobaiagent.controller;

import com.hjz.jobaiagent.app.JobApp;
import com.hjz.jobaiagent.agent.JobManus;
import com.hjz.jobaiagent.app.JobApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private JobApp jobApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    /**
     * 同步调用 AI 就业助手应用（等全部结果返回）
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping("/job_app/chat/sync")
    public String doChatWithJobAppSync(String message, String chatId) {
        return jobApp.doChat(message, chatId);
    }

    // 实现 SSE 的三种方式
    /**
     * 1. SSE 流式调用 AI 就业助手应用（仅支持文本输出）
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/job_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)  // 定义响应头为 SSE 流式传输
    public Flux<String> doChatWithJobAppSSE(String message, String chatId) {
        return jobApp.doChatByStream(message, chatId);
    }

    /**
     * 2. SSE 流式调用 AI 就业助手应用（包含更完整的 SSE 信息）
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/job_app/chat/server_sent_event")
    public Flux<ServerSentEvent<String>> doChatWithJobAppServerSentEvent(String message, String chatId) {
        return jobApp.doChatByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    /**
     * 3. SSE 流式调用 AI 就业助手应用（更灵活）
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/job_app/chat/sse_emitter")
    public SseEmitter doChatWithJobAppServerSseEmitter(String message, String chatId) {
        // 创建一个超时时间较长的 SseEmitter
        SseEmitter sseEmitter = new SseEmitter(180000L); // 3 分钟超时
        // 获取 Flux 响应式数据流并且直接通过订阅推送给 sseEmitter
        jobApp.doChatByStream(message, chatId)
                .subscribe(chunk -> {
                    try {
                        sseEmitter.send(chunk); // 服务器向前端推送消息
                    } catch (IOException e) {
                        sseEmitter.completeWithError(e);
                    }
                }, sseEmitter::completeWithError, sseEmitter::complete);    // 输出错误信息
        // 返回
        return sseEmitter;
    }

    /**
     * 流式调用 JobManus 超级智能体
     *
     * @param message
     * @return
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithJobManus(String message) {
        JobManus jobManus = new JobManus(allTools, dashscopeChatModel);
        return jobManus.runStream(message);
    }
}