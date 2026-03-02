package com.agentskills.sharing.dto;

import com.agentskills.sharing.entity.Skill;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for paginated skill list items.
 */
public record SkillListResponse(
        String id,
        String name,
        String description,
        String author,
        int downloadCount,
        int starCount,
        int forkCount,
        List<String> tags,
        LocalDateTime createdAt
) {

    public static SkillListResponse from(Skill skill) {
        List<String> tagNames = skill.getTags().stream()
                .map(t -> t.getName())
                .sorted()
                .toList();

        int starCount = 0;
        int forkCount = 0;
        if (skill.getSkillGroup() != null && skill.getSkillGroup().getRepository() != null) {
            starCount = skill.getSkillGroup().getRepository().getStarCount();
            forkCount = skill.getSkillGroup().getRepository().getForkCount();
        }

        return new SkillListResponse(
                skill.getId(),
                skill.getName(),
                skill.getDescription(),
                skill.getUser() != null ? skill.getUser().getUsername() : null,
                skill.getDownloadCount(),
                starCount,
                forkCount,
                tagNames,
                skill.getCreatedAt()
        );
    }
}
