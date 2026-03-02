package com.agentskills.sharing.dto;

import com.agentskills.sharing.entity.Skill;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for full skill detail (includes README, SkillGroup info, repo URL).
 */
public record SkillDetailResponse(
        String id,
        String name,
        String description,
        String readmeContent,
        String author,
        int downloadCount,
        int starCount,
        int forkCount,
        List<String> tags,
        String repoUrl,
        String folderPath,
        SkillGroupInfo skillGroup,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public record SkillGroupInfo(
            String id,
            String name,
            String description
    ) {
    }

    public static SkillDetailResponse from(Skill skill) {
        List<String> tagNames = skill.getTags().stream()
                .map(t -> t.getName())
                .sorted()
                .toList();

        int starCount = 0;
        int forkCount = 0;
        String repoUrl = null;
        if (skill.getSkillGroup() != null && skill.getSkillGroup().getRepository() != null) {
            starCount = skill.getSkillGroup().getRepository().getStarCount();
            forkCount = skill.getSkillGroup().getRepository().getForkCount();
            repoUrl = skill.getSkillGroup().getRepository().getUrl();
        }

        SkillGroupInfo groupInfo = null;
        if (skill.getSkillGroup() != null) {
            groupInfo = new SkillGroupInfo(
                    skill.getSkillGroup().getId(),
                    skill.getSkillGroup().getName(),
                    skill.getSkillGroup().getDescription()
            );
        }

        return new SkillDetailResponse(
                skill.getId(),
                skill.getName(),
                skill.getDescription(),
                skill.getReadmeContent(),
                skill.getUser() != null ? skill.getUser().getUsername() : null,
                skill.getDownloadCount(),
                starCount,
                forkCount,
                tagNames,
                repoUrl,
                skill.getFolderPath(),
                groupInfo,
                skill.getCreatedAt(),
                skill.getUpdatedAt()
        );
    }
}
