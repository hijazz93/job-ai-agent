package com.hjz.jobaiagent.controller;

import com.hjz.jobaiagent.app.JobApp;
import com.hjz.jobaiagent.agent.JobManus;
import com.hjz.jobaiagent.service.SessionFileContextService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private JobApp jobApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    @Resource
    private SessionFileContextService sessionFileContextService;

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
     * SSE 流式调用 AI 就业助手应用 + RAG 知识库检索
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/job_app/chat/sse_rag", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithJobAppSSEWithRag(String message, String chatId) {
        return jobApp.doChatByStreamWithRag(message, chatId);
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
     * 流式调用 JobManus 超级智能体 (POST, 消息体避免 URL 过长导致 header 溢出)
     *
     * @param body {"message": "用户消息", "tools": "逗号分隔的工具名"}
     * @return
     */
    @PostMapping("/manus/chat")
    public SseEmitter doChatWithJobManus(@RequestBody Map<String, String> body) {

        String message = body.get("message");
        String tools = body.get("tools");
        String sessionId = body.get("sessionId");
        ToolCallback[] effectiveTools = filterTools(tools);

        String fileContext = null;
        if (sessionId != null && !sessionId.isBlank()) {
            fileContext = sessionFileContextService.buildFileContextForLLM(sessionId);
        }

        JobManus jobManus = new JobManus(effectiveTools, dashscopeChatModel, fileContext);
        return jobManus.runStream(message);
    }

    /**
     * 根据用户选择过滤工具列表
     *
     * @param tools 用户选择的工具ID（逗号分隔）
     * @return 过滤后的工具数组
     */
    private ToolCallback[] filterTools(String tools) {
        if (tools == null || tools.trim().isEmpty()) {
            return allTools;
        }
        List<String> selectedToolIds = Arrays.asList(tools.split(","));
        List<ToolCallback> filtered = new ArrayList<>();
        for (ToolCallback tool : allTools) {
            String toolName = tool.getToolDefinition().name();
            if (selectedToolIds.contains(toolName)) {
                filtered.add(tool);
            }
        }
        return filtered.isEmpty() ? allTools : filtered.toArray(new ToolCallback[0]);
    }
}