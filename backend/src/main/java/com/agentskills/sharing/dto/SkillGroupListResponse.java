package com.agentskills.sharing.dto;

import com.agentskills.sharing.entity.SkillGroup;

import java.time.LocalDateTime;

/**
 * Response DTO for skill group list items.
 */
public record SkillGroupListResponse(
        String id,
        String name,
        String description,
        String author,
        int downloadCount,
        int skillCount,
        int starCount,
        int forkCount,
        String repoUrl,
        LocalDateTime createdAt
) {

    public static SkillGroupListResponse from(SkillGroup group, int skillCount) {
        int starCount = 0;
        int forkCount = 0;
        String repoUrl = null;
        if (group.getRepository() != null) {
            starCount = group.getRepository().getStarCount();
            forkCount = group.getRepository().getForkCount();
            repoUrl = group.getRepository().getUrl();
        }
        return new SkillGroupListResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getUser() != null ? group.getUser().getUsername() : null,
                group.getDownloadCount(),
                skillCount,
                starCount,
                forkCount,
                repoUrl,
                group.getCreatedAt()
        );
    }
}
