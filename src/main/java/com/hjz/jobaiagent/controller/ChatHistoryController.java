package com.hjz.jobaiagent.controller;

import com.hjz.jobaiagent.dto.ApiResponse;
import com.hjz.jobaiagent.entity.ChatMessage;
import com.hjz.jobaiagent.entity.ChatSession;
import com.hjz.jobaiagent.entity.SessionFileContext;
import com.hjz.jobaiagent.security.CustomUserDetails;
import com.hjz.jobaiagent.service.ChatHistoryService;
import com.hjz.jobaiagent.service.SessionFileContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final ChatHistoryService chatHistoryService;
    private final SessionFileContextService sessionFileContextService;

    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<?>> createSession(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String id = body.get("id");
        String title = body.get("title");
        ChatSession session = chatHistoryService.createSession(id, userDetails.getId(), title);
        Map<String, Object> result = new HashMap<>();
        result.put("id", session.getId());
        result.put("title", session.getTitle());
        result.put("createdAt", session.getCreatedAt());
        result.put("updatedAt", session.getUpdatedAt());
        return ResponseEntity.ok(ApiResponse.ok("创建成功", result));
    }

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSessions(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ChatSession> sessions = chatHistoryService.getUserSessions(userDetails.getId());
        List<Map<String, Object>> result = sessions.stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("title", s.getTitle());
            map.put("createdAt", s.getCreatedAt());
            map.put("updatedAt", s.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PutMapping("/sessions/{id}")
    public ResponseEntity<ApiResponse<?>> updateSessionTitle(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        chatHistoryService.updateSessionTitle(id, body.get("title"));
        return ResponseEntity.ok(ApiResponse.ok("更新成功"));
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<ApiResponse<?>> deleteSession(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        chatHistoryService.deleteSession(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("删除成功"));
    }

    @GetMapping("/sessions/{id}/messages")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSessionMessages(
            @PathVariable String id) {
        List<ChatMessage> messages = chatHistoryService.getSessionMessages(id);
        List<Map<String, Object>> result = messages.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("role", m.getRole());
            map.put("content", m.getContent());
            map.put("createdAt", m.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/sessions/{id}/messages")
    public ResponseEntity<ApiResponse<?>> saveMessage(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails != null ? userDetails.getId() : null;
        ChatMessage message = chatHistoryService.saveMessage(id, userId, body.get("role"), body.get("content"));
        Map<String, Object> result = new HashMap<>();
        result.put("id", message.getId());
        result.put("role", message.getRole());
        result.put("content", message.getContent());
        result.put("createdAt", message.getCreatedAt());
        return ResponseEntity.ok(ApiResponse.ok("保存成功", result));
    }

    @PostMapping("/sessions/{id}/files")
    public ResponseEntity<ApiResponse<?>> addSessionFile(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        String originalFileName = (String) body.get("originalFileName");
        String uniqueFileName = (String) body.get("uniqueFileName");
        String fullPath = (String) body.get("fullPath");
        String content = (String) body.get("content");
        int charCount = body.get("charCount") != null ? ((Number) body.get("charCount")).intValue() : 0;

        SessionFileContext ctx = sessionFileContextService.addFileContext(
                id, originalFileName, uniqueFileName, fullPath, content, charCount);

        Map<String, Object> result = new HashMap<>();
        result.put("id", ctx.getId());
        result.put("originalFileName", ctx.getOriginalFileName());
        result.put("uniqueFileName", ctx.getUniqueFileName());
        result.put("fullPath", ctx.getFullPath());
        result.put("charCount", ctx.getCharCount());
        result.put("uploadedAt", ctx.getUploadedAt());
        return ResponseEntity.ok(ApiResponse.ok("文件关联成功", result));
    }

    @GetMapping("/sessions/{id}/files")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSessionFiles(
            @PathVariable String id) {
        List<SessionFileContext> files = sessionFileContextService.getFileContexts(id);
        List<Map<String, Object>> result = files.stream().map(f -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", f.getId());
            map.put("originalFileName", f.getOriginalFileName());
            map.put("uniqueFileName", f.getUniqueFileName());
            map.put("fullPath", f.getFullPath());
            map.put("charCount", f.getCharCount());
            map.put("uploadedAt", f.getUploadedAt());
            // 列表接口只返回前 200 字符预览，不返回完整内容
            String c = f.getContent();
            map.put("contentPreview", c != null && c.length() > 200 ? c.substring(0, 200) + "..." : c);
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @DeleteMapping("/sessions/{id}/files/{fileId}")
    public ResponseEntity<ApiResponse<?>> removeSessionFile(
            @PathVariable String id,
            @PathVariable Long fileId) {
        sessionFileContextService.removeFileContext(id, fileId);
        return ResponseEntity.ok(ApiResponse.ok("文件移除成功"));
    }
}
