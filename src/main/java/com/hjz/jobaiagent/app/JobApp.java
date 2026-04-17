package com.hjz.jobaiagent.app;


import com.hjz.jobaiagent.advisor.MyLoggerAdvisor;
import com.hjz.jobaiagent.advisor.ReReadingAdvisor;
import com.hjz.jobaiagent.chatmemory.FileBasedChatMemory;
import com.hjz.jobaiagent.rag.JobAppRagCustomAdvisorFactory;
import com.hjz.jobaiagent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j

public class JobApp {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = "你是专业、严谨、共情的就业咨询专家，专注为用户解决全流程就业问题，涵盖职业规划、简历优化、面试辅导、行业选择、职场适应、升学就业权衡等全维度就业相关咨询，始终以用户需求为核心，提供落地、可执行的专业建议。";

    /**
     * 构造函数，初始化大模型客户端
     * @param dashscopeChatModel 大模型
     */
    public JobApp(ChatModel dashscopeChatModel) {
        // 基于文件的对话记忆
        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);

        // 基于内存的对话记忆
//        ChatMemory chatMemory = new InMemoryChatMemory();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)   // 系统预设
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory)    // 默认拦截器
                        // 自定义拦截器
                        , new MyLoggerAdvisor()
                        // 自定义 Re2 Advisor （重读），按需开启
//                        , new ReReadingAdvisor()
                )
                .build();
    }

    /**
     * 用户进行对话，传入用户消息和对话ID，返回模型回复。支持多轮对话
     * @param message 用户消息
     * @param chatId 对话ID
     * @return 模型回复内容
     */
    public String doChat(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)   //spec相当于Map
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


    // 定义一个记录，为就业指导报告，包含标题和建议的列表
    record JobReport(String title, List<String> suggestions) {

    }

    /**
     * AI 就业指导报告生成，演示结构化输出功能
     * @param message
     * @param chatId
     * @return
     */
    public JobReport doChatWithReport(String message, String chatId) {
        JobReport jobReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成就业指导结果，标题为{用户名}的就业指导报告，内容建议为列表")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)   //spec相当于Map
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(JobReport.class);
        log.info("jobReport: {}", jobReport);
        return jobReport;
    }

    // AI 就业本地文档问答
    @Resource
    private VectorStore jobAppVectorStore;

    // 云端 RAG 检索增强服务（阿里百炼平台）
    @Resource
    private Advisor jobAppRagCloudAdvisor;

    // PGvector 向量存储 RAG 检索增强服务
    @Resource
    private VectorStore pgVectorVectorStore;

    @Resource
    private QueryRewriter queryRewriter;

    /**
     * RAG 知识库问答
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithRag(String message, String chatId) {
        // 执行查询重写
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);

        ChatResponse response = chatClient
                .prompt()
                // 重写后的用户消息
                .user(rewrittenMessage)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)   //spec相当于Map
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于调试
                .advisors(new MyLoggerAdvisor())

                // 应用 RAG 本地文档问答
                .advisors(new QuestionAnswerAdvisor(jobAppVectorStore))
                // 应用 RAG 检索增强服务（基于云端阿里百炼平台）
//                .advisors(jobAppRagCloudAdvisor)
                // 应用 RAG 检索增强服务（基于 PGvector 向量存储）
//                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                // 应用自定义的 RAG 检索增强顾问（文档查询 + 上下文增强）
//                .advisors(JobAppRagCustomAdvisorFactory.createJobAppRagCustomAdvisor(jobAppVectorStore,"美食"))

                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
}
