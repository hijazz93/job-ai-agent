package com.hjz.jobaiagent.controller;

import com.hjz.jobaiagent.dto.ApiResponse;
import com.hjz.jobaiagent.dto.UpgradeRequestDto;
import com.hjz.jobaiagent.entity.KnowledgeDocument;
import com.hjz.jobaiagent.entity.User;
import com.hjz.jobaiagent.security.CustomUserDetails;
import com.hjz.jobaiagent.service.AdminService;
import com.hjz.jobaiagent.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final KnowledgeBaseService knowledgeBaseService;

    @GetMapping("/upgrade-requests")
    public ResponseEntity<ApiResponse<List<UpgradeRequestDto>>> getUpgradeRequests(
            @RequestParam(required = false) String status) {
        List<UpgradeRequestDto> requests = adminService.getUpgradeRequests(status);
        return ResponseEntity.ok(ApiResponse.ok(requests));
    }

    @PostMapping("/upgrade-requests/{id}/approve")
    public ResponseEntity<ApiResponse<?>> approveUpgradeRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails admin) {
        try {
            adminService.approveUpgradeRequest(id, admin.getId());
            return ResponseEntity.ok(ApiResponse.ok("已批准该升级申请"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/upgrade-requests/{id}/reject")
    public ResponseEntity<ApiResponse<?>> rejectUpgradeRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails admin) {
        try {
            adminService.rejectUpgradeRequest(id, admin.getId());
            return ResponseEntity.ok(ApiResponse.ok("已拒绝该升级申请"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        Page<User> result = adminService.listUsersPageable(page, size, keyword);
        List<Map<String, Object>> userList = result.getContent().stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("username", user.getUsername());
            map.put("email", user.getEmail());
            map.put("role", user.getRole().name());
            map.put("enabled", user.isEnabled());
            map.put("createdAt", user.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("content", userList);
        data.put("totalElements", result.getTotalElements());
        data.put("totalPages", result.getTotalPages());
        data.put("number", result.getNumber());
        data.put("size", result.getSize());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<?>> createUser(@RequestBody Map<String, Object> body) {
        try {
            String username = (String) body.get("username");
            String email = (String) body.get("email");
            String password = (String) body.get("password");
            String role = (String) body.get("role");
            boolean enabled = body.containsKey("enabled") ? Boolean.TRUE.equals(body.get("enabled")) : true;

            if (username == null || username.isBlank()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户名不能为空"));
            }
            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("邮箱不能为空"));
            }
            if (password == null || password.length() < 6) {
                return ResponseEntity.badRequest().body(ApiResponse.error("密码至少6位"));
            }

            User user = adminService.createUser(username, email, password,
                    role != null ? role : "NORMAL_USER", enabled);

            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("username", user.getUsername());
            map.put("email", user.getEmail());
            map.put("role", user.getRole().name());
            map.put("enabled", user.isEnabled());
            return ResponseEntity.ok(ApiResponse.ok("创建成功", map));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<?>> updateUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            User user = adminService.updateUser(id,
                    (String) body.get("username"),
                    (String) body.get("email"),
                    (String) body.get("role"),
                    body.containsKey("enabled") ? Boolean.TRUE.equals(body.get("enabled")) : null);

            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("username", user.getUsername());
            map.put("email", user.getEmail());
            map.put("role", user.getRole().name());
            map.put("enabled", user.isEnabled());
            return ResponseEntity.ok(ApiResponse.ok("更新成功", map));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<?>> deleteUser(@PathVariable Long id) {
        try {
            adminService.deleteUser(id);
            return ResponseEntity.ok(ApiResponse.ok("删除成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ====== 知识库管理 CRUD ======

    @GetMapping("/knowledge")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listKnowledge(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        Page<KnowledgeDocument> result = knowledgeBaseService.syncAndList(page, size, keyword);
        Map<String, Object> data = new HashMap<>();
        data.put("content", result.getContent());
        data.put("totalElements", result.getTotalElements());
        data.put("totalPages", result.getTotalPages());
        data.put("number", result.getNumber());
        data.put("size", result.getSize());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/knowledge/{id}")
    public ResponseEntity<ApiResponse<KnowledgeDocument>> getKnowledge(@PathVariable Long id) {
        try {
            KnowledgeDocument doc = knowledgeBaseService.getDocument(id);
            return ResponseEntity.ok(ApiResponse.ok(doc));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/knowledge")
    public ResponseEntity<ApiResponse<KnowledgeDocument>> createKnowledge(@RequestBody Map<String, String> body) {
        String title = body.get("title");
        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("标题不能为空"));
        }
        KnowledgeDocument doc = knowledgeBaseService.createDocument(
                title,
                body.get("content"),
                body.get("description"),
                body.get("status")
        );
        return ResponseEntity.ok(ApiResponse.ok("创建成功", doc));
    }

    @PutMapping("/knowledge/{id}")
    public ResponseEntity<ApiResponse<KnowledgeDocument>> updateKnowledge(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            KnowledgeDocument doc = knowledgeBaseService.updateDocument(
                    id,
                    (String) body.get("title"),
                    (String) body.get("content"),
                    (String) body.get("description"),
                    (String) body.get("status"),
                    body.containsKey("enabled") ? Boolean.valueOf(String.valueOf(body.get("enabled"))) : null
            );
            return ResponseEntity.ok(ApiResponse.ok("更新成功", doc));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/knowledge/{id}")
    public ResponseEntity<ApiResponse<?>> deleteKnowledge(@PathVariable Long id) {
        try {
            knowledgeBaseService.deleteDocument(id);
            return ResponseEntity.ok(ApiResponse.ok("删除成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/knowledge/rebuild")
    public ResponseEntity<ApiResponse<?>> rebuildKnowledgeIndex() {
        try {
            knowledgeBaseService.rebuildVectorStore();
            return ResponseEntity.ok(ApiResponse.ok("知识库索引重建成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("重建失败: " + e.getMessage()));
        }
    }
}
