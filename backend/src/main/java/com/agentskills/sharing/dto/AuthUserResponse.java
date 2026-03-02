package com.agentskills.sharing.dto;

/**
 * Response DTO for the /api/auth/me endpoint.
 * Returns the authenticated user's basic profile information.
 */
public record AuthUserResponse(
        String id,
        String username,
        String displayName,
        String avatarUrl
) {
}
