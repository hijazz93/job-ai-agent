package com.hjz.jobaiagent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UpgradeRequestDto {
    private Long id;
    private Long userId;
    private String username;
    private String email;
    private String status;
    private LocalDateTime createdAt;
}
