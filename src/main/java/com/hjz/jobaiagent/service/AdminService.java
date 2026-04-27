package com.hjz.jobaiagent.service;

import com.hjz.jobaiagent.dto.UpgradeRequestDto;
import com.hjz.jobaiagent.entity.Role;
import com.hjz.jobaiagent.entity.UpgradeRequest;
import com.hjz.jobaiagent.entity.User;
import com.hjz.jobaiagent.repository.UpgradeRequestRepository;
import com.hjz.jobaiagent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UpgradeRequestRepository upgradeRequestRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UpgradeRequestDto> getUpgradeRequests(String status) {
        List<UpgradeRequest> requests;
        if (status != null && !status.isEmpty()) {
            requests = upgradeRequestRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase());
        } else {
            requests = upgradeRequestRepository.findAll();
            requests.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        }
        return requests.stream()
                .map(req -> new UpgradeRequestDto(
                        req.getId(),
                        req.getUser().getId(),
                        req.getUser().getUsername(),
                        req.getUser().getEmail(),
                        req.getStatus(),
                        req.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void approveUpgradeRequest(Long requestId, Long adminId) {
        UpgradeRequest request = upgradeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("申请不存在"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("管理员不存在"));

        User user = request.getUser();
        user.setRole(Role.PREMIUM_USER);
        userRepository.save(user);

        request.setStatus("APPROVED");
        request.setReviewedBy(admin);
        request.setReviewedAt(LocalDateTime.now());
        upgradeRequestRepository.save(request);
    }

    @Transactional
    public void rejectUpgradeRequest(Long requestId, Long adminId) {
        UpgradeRequest request = upgradeRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("申请不存在"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("管理员不存在"));

        request.setStatus("REJECTED");
        request.setReviewedBy(admin);
        request.setReviewedAt(LocalDateTime.now());
        upgradeRequestRepository.save(request);
    }

    // ========== 用户管理 CRUD ==========

    public Page<User> listUsersPageable(int page, int size, String keyword) {
        PageRequest pageRequest = PageRequest.of(page, size);
        if (keyword != null && !keyword.isBlank()) {
            return userRepository.findByUsernameContainingOrEmailContaining(keyword, keyword, pageRequest);
        }
        return userRepository.findAll(pageRequest);
    }

    @Transactional
    public User createUser(String username, String email, String password, String role, boolean enabled) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("邮箱已被使用");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.valueOf(role));
        user.setEnabled(enabled);
        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, String username, String email, String role, Boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (username != null && !username.equals(user.getUsername())) {
            if (userRepository.existsByUsername(username)) {
                throw new RuntimeException("用户名已存在");
            }
            user.setUsername(username);
        }
        if (email != null && !email.equals(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new RuntimeException("邮箱已被使用");
            }
            user.setEmail(email);
        }
        if (role != null) user.setRole(Role.valueOf(role));
        if (enabled != null) user.setEnabled(enabled);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (user.getRole() == Role.ADMIN) {
            throw new RuntimeException("不能删除管理员账号");
        }
        userRepository.deleteById(id);
    }
}
