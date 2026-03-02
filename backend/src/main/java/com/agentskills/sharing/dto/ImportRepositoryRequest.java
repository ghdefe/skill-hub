package com.agentskills.sharing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for importing a GitHub repository.
 */
public record ImportRepositoryRequest(
        @NotBlank(message = "仓库 URL 不能为空")
        @Pattern(regexp = "^https://github\\.com/[^/]+/[^/]+.*$", message = "仓库 URL 格式无效")
        String url
) {
}
