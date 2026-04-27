package com.hjz.jobaiagent.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "session_file_contexts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionFileContext {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String sessionId;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String uniqueFileName;

    private String fullPath;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Integer charCount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}
