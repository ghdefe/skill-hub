package com.agentskills.sharing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing a single item from the GitHub Contents API response.
 */
public record GitHubContentItem(
        String name,
        String type,
        String path,
        @JsonProperty("download_url") String downloadUrl
) {
}
