package com.hjz.jobaiagent.repository;

import com.hjz.jobaiagent.entity.UpgradeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UpgradeRequestRepository extends JpaRepository<UpgradeRequest, Long> {
    List<UpgradeRequest> findByStatusOrderByCreatedAtDesc(String status);
    List<UpgradeRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<UpgradeRequest> findTopByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
    boolean existsByUserIdAndStatus(Long userId, String status);
}
