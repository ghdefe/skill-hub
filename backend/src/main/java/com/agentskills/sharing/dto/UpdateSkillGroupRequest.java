package com.agentskills.sharing.dto;

import jakarta.validation.constraints.Size;

/**
 * Request DTO for editing a SkillGroup's name and description.
 * Both fields are optional — only provided fields will be updated.
 */
public record UpdateSkillGroupRequest(
        @Size(min = 1, max = 255, message = "名称长度必须在 1-255 之间")
        String name,

        @Size(max = 1000, message = "描述长度不能超过 1000")
        String description
) {
}
