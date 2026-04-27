package com.hjz.jobaiagent.service;

import com.hjz.jobaiagent.entity.SessionFileContext;
import com.hjz.jobaiagent.repository.SessionFileContextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionFileContextService {

    private final SessionFileContextRepository repository;

    private static final int MAX_CONTENT_CHARS_PER_FILE = 50000;

    @Transactional
    public SessionFileContext addFileContext(String sessionId, String originalFileName,
            String uniqueFileName, String fullPath, String content, int charCount) {
        // 同一会话相同文件去重：更新已有记录
        if (repository.existsBySessionIdAndUniqueFileName(sessionId, uniqueFileName)) {
            List<SessionFileContext> existing = repository.findBySessionIdOrderByUploadedAtAsc(sessionId);
            for (SessionFileContext ctx : existing) {
                if (ctx.getUniqueFileName().equals(uniqueFileName)) {
                    ctx.setContent(content);
                    ctx.setCharCount(charCount);
                    ctx.setFullPath(fullPath);
                    return repository.save(ctx);
                }
            }
        }
        SessionFileContext ctx = new SessionFileContext();
        ctx.setSessionId(sessionId);
        ctx.setOriginalFileName(originalFileName);
        ctx.setUniqueFileName(uniqueFileName);
        ctx.setFullPath(fullPath);
        ctx.setContent(content);
        ctx.setCharCount(charCount);
        return repository.save(ctx);
    }

    public List<SessionFileContext> getFileContexts(String sessionId) {
        return repository.findBySessionIdOrderByUploadedAtAsc(sessionId);
    }

    @Transactional
    public void removeFileContext(String sessionId, Long fileId) {
        repository.deleteBySessionIdAndId(sessionId, fileId);
    }

    @Transactional
    public void removeAllFileContexts(String sessionId) {
        repository.deleteBySessionId(sessionId);
    }

    /**
     * 构建注入 LLM 系统提示词的文件上下文字符串
     */
    public String buildFileContextForLLM(String sessionId) {
        List<SessionFileContext> contexts = getFileContexts(sessionId);
        if (contexts.isEmpty()) return null;

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n=== 用户已上传的文件内容 ===\n");
        for (int i = 0; i < contexts.size(); i++) {
            SessionFileContext ctx = contexts.get(i);
            sb.append("\n【文件 ").append(i + 1).append("】").append(ctx.getOriginalFileName()).append("\n");
            sb.append("--- 内容开始 ---\n");
            String content = ctx.getContent();
            if (content != null && content.length() > MAX_CONTENT_CHARS_PER_FILE) {
                sb.append(content, 0, MAX_CONTENT_CHARS_PER_FILE);
                sb.append("\n[文件内容已截断，超出 ").append(MAX_CONTENT_CHARS_PER_FILE)
                  .append(" 字符。如需完整内容请使用 readFile 工具读取：")
                  .append(ctx.getFullPath()).append("]");
            } else {
                sb.append(content != null ? content : "");
            }
            sb.append("\n--- 内容结束 ---\n");
        }
        return sb.toString();
    }
}
