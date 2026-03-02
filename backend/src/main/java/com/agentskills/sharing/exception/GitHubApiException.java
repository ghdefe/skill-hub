package com.agentskills.sharing.exception;

/**
 * Exception thrown when a GitHub API call fails (rate limit, not found, network error, etc.).
 */
public class GitHubApiException extends RuntimeException {

    private final int statusCode;

    public GitHubApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public GitHubApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 500;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
