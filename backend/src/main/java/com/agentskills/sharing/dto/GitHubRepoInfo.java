package com.agentskills.sharing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing repository metadata from the GitHub Repos API.
 */
public record GitHubRepoInfo(
        @JsonProperty("stargazers_count") int stargazersCount,
        @JsonProperty("forks_count") int forksCount
) {
}
