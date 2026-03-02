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
        LocalDateTime createdAt
) {

    public static SkillGroupListResponse from(SkillGroup group) {
        return new SkillGroupListResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getUser() != null ? group.getUser().getUsername() : null,
                group.getDownloadCount(),
                group.getCreatedAt()
        );
    }
}
