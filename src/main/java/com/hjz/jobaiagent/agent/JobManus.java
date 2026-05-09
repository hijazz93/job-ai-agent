package com.hjz.jobaiagent.agent;

import com.hjz.jobaiagent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
/**
 * 就业 AI 超级智能体（拥有自主规划能力，可以直接使用）
 * 注意：此类由 AiController 手动实例化，不作为 Spring Bean 管理
 */
public class JobManus extends ToolCallAgent {

    public JobManus(ToolCallback[] allTools, ChatModel dashscopeChatModel, String fileContext) {
        super(allTools);
        this.setName("jobManus");
        String SYSTEM_PROMPT = """
                You are JobManus, an all-capable AI assistant, aimed at solving any task presented by the user.
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.
                """;
        if (fileContext != null && !fileContext.isBlank()) {
            this.setSystemPrompt(SYSTEM_PROMPT + fileContext);
        } else {
            this.setSystemPrompt(SYSTEM_PROMPT);
        }
        String NEXT_STEP_PROMPT = """
                请始终使用中文回复用户。
                根据用户需求，主动选择最合适的工具或工具组合。
                对于复杂任务，可以分解问题，逐步使用不同工具来解决。
                每次使用工具后，清楚地解释执行结果，并建议下一步操作。
                如果你想停止交互，请使用 `terminate` 工具/函数调用。
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(20);

        // 初始化 AI 对话客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}