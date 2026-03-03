package com.agentskills.sharing.dto;

import com.agentskills.sharing.entity.Repository;
import com.agentskills.sharing.entity.SkillGroup;

import java.time.LocalDateTime;

/**
 * Response DTO for Repository API endpoints.
 * Includes basic repository info and associated SkillGroup summary.
 */
public record RepositoryResponse(
        String id,
        String githubOwner,
        String githubRepo,
        String url,
        int starCount,
        int forkCount,
        String scanPath,
        String scanBranch,
        LocalDateTime lastSyncedAt,
        LocalDateTime createdAt,
        SkillGroupSummary skillGroup
) {

    public record SkillGroupSummary(
            String id,
            String name,
            int skillCount
    ) {
    }

    public static RepositoryResponse from(Repository repo, SkillGroup group, int skillCount) {
        SkillGroupSummary groupSummary = group != null
                ? new SkillGroupSummary(group.getId(), group.getName(), skillCount)
                : null;

        return new RepositoryResponse(
                repo.getId(),
                repo.getGithubOwner(),
                repo.getGithubRepo(),
                repo.getUrl(),
                repo.getStarCount(),
                repo.getForkCount(),
                repo.getScanPath(),
                repo.getScanBranch(),
                repo.getLastSyncedAt(),
                repo.getCreatedAt(),
                groupSummary
        );
    }
}
