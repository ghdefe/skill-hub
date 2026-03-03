package com.agentskills.sharing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "repositories", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "github_owner", "github_repo"})
})
@Data
public class Repository {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "github_owner", nullable = false)
    private String githubOwner;

    @Column(name = "github_repo", nullable = false)
    private String githubRepo;

    @Column(nullable = false)
    private String url;

    @Column(name = "star_count", nullable = false)
    private Integer starCount = 0;

    @Column(name = "fork_count", nullable = false)
    private Integer forkCount = 0;

    @Column(name = "webhook_id")
    private String webhookId;

    @Column(name = "webhook_secret")
    private String webhookSecret;

    @Column(name = "default_branch")
    private String defaultBranch;

    @Column(name = "scan_path", nullable = false)
    private String scanPath = "skills";

    @Column(name = "scan_branch")
    private String scanBranch;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
