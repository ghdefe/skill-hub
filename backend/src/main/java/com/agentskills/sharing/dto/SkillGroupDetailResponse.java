package com.agentskills.sharing.dto;

import com.agentskills.sharing.entity.Skill;
import com.agentskills.sharing.entity.SkillGroup;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for skill group detail (includes skill list and total download count).
 */
public record SkillGroupDetailResponse(
        String id,
        String name,
        String description,
        String author,
        String repoUrl,
        int downloadCount,
        int totalSkillDownloads,
        List<SkillSummary> skills,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public record SkillSummary(
            String id,
            String name,
            String description,
            int downloadCount,
            String status,
            String folderPath
    ) {
        public static SkillSummary from(Skill skill) {
            return new SkillSummary(
                    skill.getId(),
                    skill.getName(),
                    skill.getDescription(),
                    skill.getDownloadCount(),
                    skill.getStatus().name(),
                    skill.getFolderPath()
            );
        }
    }

    public static SkillGroupDetailResponse from(SkillGroup group, List<Skill> skills) {
        List<SkillSummary> skillSummaries = skills.stream()
                .map(SkillSummary::from)
                .toList();

        int totalSkillDownloads = skills.stream()
                .mapToInt(Skill::getDownloadCount)
                .sum();

        String repoUrl = null;
        if (group.getRepository() != null) {
            repoUrl = group.getRepository().getUrl();
        }

        return new SkillGroupDetailResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getUser() != null ? group.getUser().getUsername() : null,
                repoUrl,
                group.getDownloadCount(),
                totalSkillDownloads,
                skillSummaries,
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }
}
