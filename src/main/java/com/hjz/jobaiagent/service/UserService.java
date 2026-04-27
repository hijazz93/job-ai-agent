package com.hjz.jobaiagent.service;

import com.hjz.jobaiagent.entity.UpgradeRequest;
import com.hjz.jobaiagent.entity.User;
import com.hjz.jobaiagent.repository.UpgradeRequestRepository;
import com.hjz.jobaiagent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UpgradeRequestRepository upgradeRequestRepository;

    public User getProfile(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    @Transactional
    public UpgradeRequest submitUpgradeRequest(Long userId) {
        // 检查是否已有待处理的申请
        if (upgradeRequestRepository.existsByUserIdAndStatus(userId, "PENDING")) {
            throw new RuntimeException("已有待处理的升级申请，请耐心等待审核");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 检查用户是否已经是高级用户或管理员
        if (user.getRole().name().equals("PREMIUM_USER") || user.getRole().name().equals("ADMIN")) {
            throw new RuntimeException("您已经是高级用户，无需再次申请");
        }

        UpgradeRequest request = new UpgradeRequest();
        request.setUser(user);
        request.setStatus("PENDING");
        return upgradeRequestRepository.save(request);
    }

    public Optional<UpgradeRequest> getUpgradeRequestStatus(Long userId) {
        return upgradeRequestRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(userId, "PENDING");
    }

    public Optional<UpgradeRequest> getLatestUpgradeRequest(Long userId) {
        return upgradeRequestRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().findFirst();
    }
}
