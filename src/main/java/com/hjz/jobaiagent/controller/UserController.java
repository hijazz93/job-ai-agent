package com.hjz.jobaiagent.controller;

import com.hjz.jobaiagent.dto.ApiResponse;
import com.hjz.jobaiagent.dto.UpgradeRequestDto;
import com.hjz.jobaiagent.entity.UpgradeRequest;
import com.hjz.jobaiagent.entity.User;
import com.hjz.jobaiagent.security.CustomUserDetails;
import com.hjz.jobaiagent.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userService.getProfile(userDetails.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("email", user.getEmail());
        data.put("role", user.getRole().name());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping("/upgrade-request")
    public ResponseEntity<ApiResponse<?>> submitUpgradeRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            userService.submitUpgradeRequest(userDetails.getId());
            return ResponseEntity.ok(ApiResponse.ok("升级申请已提交，请等待管理员审核"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/upgrade-request/status")
    public ResponseEntity<ApiResponse<?>> getUpgradeRequestStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Optional<UpgradeRequest> request = userService.getLatestUpgradeRequest(userDetails.getId());
        if (request.isPresent()) {
            UpgradeRequest r = request.get();
            UpgradeRequestDto dto = new UpgradeRequestDto(
                    r.getId(), r.getUser().getId(), r.getUser().getUsername(),
                    r.getUser().getEmail(), r.getStatus(), r.getCreatedAt());
            return ResponseEntity.ok(ApiResponse.ok(dto));
        }
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
