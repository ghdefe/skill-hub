package com.agentskills.sharing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for importing a GitHub repository.
 */
public record ImportRepositoryRequest(
        @NotBlank(message = "仓库 URL 不能为空")
        @Pattern(regexp = "^https://github\\.com/[^/]+/[^/]+.*$", message = "仓库 URL 格式无效")
        String url,

        String scanPath,

        String scanBranch
) {
    /**
     * Return the scan path, defaulting to "skills" if not provided.
     * Converts "." to empty string for GitHub API root directory access.
     */
    public String effectiveScanPath() {
        if (scanPath == null || scanPath.isBlank()) {
            return "skills";
        }
        String trimmed = scanPath.trim();
        // GitHub API uses empty string for root directory, not "."
        return ".".equals(trimmed) ? "" : trimmed;
    }

    /**
     * Return the scan branch, or null to use the repo's default branch.
     */
    public String effectiveScanBranch() {
        return (scanBranch != null && !scanBranch.isBlank()) ? scanBranch.trim() : null;
    }
}
