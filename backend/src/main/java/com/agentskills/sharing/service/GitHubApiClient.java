package com.agentskills.sharing.service;

import com.agentskills.sharing.dto.GitHubContentItem;
import com.agentskills.sharing.dto.GitHubRepoInfo;
import com.agentskills.sharing.exception.GitHubApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Service that wraps GitHub REST API calls using Spring WebClient.
 * Integrates Caffeine caching and handles rate-limit headers.
 */
@Service
@Slf4j
public class GitHubApiClient {

    private static final int RATE_LIMIT_THRESHOLD = 10;

    private final WebClient gitHubWebClient;

    public GitHubApiClient(WebClient gitHubWebClient) {
        this.gitHubWebClient = gitHubWebClient;
    }

    /**
     * List contents of a directory in a GitHub repository.
     *
     * @param owner repository owner
     * @param repo  repository name
     * @param path  directory path (e.g. "skills")
     * @param token GitHub access token
     * @return list of content items in the directory
     */
    @Cacheable(value = "githubRepoContents", key = "#owner + '/' + #repo + '/' + #path")
    public List<GitHubContentItem> getRepoContents(String owner, String repo, String path, String token) {
        log.debug("Fetching repo contents: {}/{}/{}", owner, repo, path);
        return gitHubWebClient.get()
                .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, path)
                .headers(headers -> addAuthHeader(headers, token))
                .exchangeToMono(response -> handleContentsResponse(response, owner, repo, path))
                .block();
    }

    /**
     * Read a single file's content from a GitHub repository (Base64 decoded).
     *
     * @param owner repository owner
     * @param repo  repository name
     * @param path  file path (e.g. "skills/my-skill/README.md")
     * @param token GitHub access token
     * @return decoded file content as a string
     */
    @Cacheable(value = "githubFileContent", key = "#owner + '/' + #repo + '/' + #path")
    public String getFileContent(String owner, String repo, String path, String token) {
        log.debug("Fetching file content: {}/{}/{}", owner, repo, path);
        return gitHubWebClient.get()
                .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, path)
                .headers(headers -> addAuthHeader(headers, token))
                .exchangeToMono(response -> handleFileContentResponse(response, owner, repo, path))
                .block();
    }

    /**
     * Get repository metadata (star count, fork count).
     *
     * @param owner repository owner
     * @param repo  repository name
     * @param token GitHub access token
     * @return repository info DTO
     */
    @Cacheable(value = "githubRepoInfo", key = "#owner + '/' + #repo")
    public GitHubRepoInfo getRepoInfo(String owner, String repo, String token) {
        log.debug("Fetching repo info: {}/{}", owner, repo);
        return gitHubWebClient.get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .headers(headers -> addAuthHeader(headers, token))
                .exchangeToMono(response -> handleRepoInfoResponse(response, owner, repo))
                .block();
    }

    // ---- Private helpers ----

    private void addAuthHeader(HttpHeaders headers, String token) {
        if (token != null && !token.isBlank()) {
            headers.setBearerAuth(token);
        }
    }

    private void checkRateLimit(ClientResponse response) {
        String remaining = response.headers().asHttpHeaders().getFirst("X-RateLimit-Remaining");
        if (remaining != null) {
            int remainingCount = Integer.parseInt(remaining);
            if (remainingCount <= RATE_LIMIT_THRESHOLD) {
                log.warn("GitHub API rate limit approaching: {} requests remaining", remainingCount);
            }
            if (remainingCount == 0) {
                String resetHeader = response.headers().asHttpHeaders().getFirst("X-RateLimit-Reset");
                log.error("GitHub API rate limit exhausted. Resets at epoch: {}", resetHeader);
            }
        }
    }

    private Mono<List<GitHubContentItem>> handleContentsResponse(ClientResponse response, String owner, String repo, String path) {
        checkRateLimit(response);
        HttpStatusCode status = response.statusCode();

        if (status.is2xxSuccessful()) {
            return response.bodyToMono(new ParameterizedTypeReference<>() {});
        }
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(body -> Mono.error(mapError(status.value(), owner, repo, path, body)));
    }

    private Mono<String> handleFileContentResponse(ClientResponse response, String owner, String repo, String path) {
        checkRateLimit(response);
        HttpStatusCode status = response.statusCode();

        if (status.is2xxSuccessful()) {
            return response.bodyToMono(Map.class).map(body -> {
                String encoding = (String) body.get("encoding");
                String content = (String) body.get("content");
                if ("base64".equals(encoding) && content != null) {
                    String cleaned = content.replaceAll("\\s", "");
                    return new String(Base64.getDecoder().decode(cleaned), StandardCharsets.UTF_8);
                }
                return content != null ? content : "";
            });
        }
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(body -> Mono.error(mapError(status.value(), owner, repo, path, body)));
    }

    private Mono<GitHubRepoInfo> handleRepoInfoResponse(ClientResponse response, String owner, String repo) {
        checkRateLimit(response);
        HttpStatusCode status = response.statusCode();

        if (status.is2xxSuccessful()) {
            return response.bodyToMono(GitHubRepoInfo.class);
        }
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(body -> Mono.error(mapError(status.value(), owner, repo, null, body)));
    }

    private GitHubApiException mapError(int statusCode, String owner, String repo, String path, String body) {
        String target = path != null ? owner + "/" + repo + "/" + path : owner + "/" + repo;
        return switch (statusCode) {
            case 404 -> new GitHubApiException("GitHub resource not found: " + target, 404);
            case 403 -> new GitHubApiException("GitHub API rate limit exceeded or access forbidden: " + target, 403);
            case 401 -> new GitHubApiException("GitHub authentication failed for: " + target, 401);
            default -> {
                log.error("GitHub API error {} for {}: {}", statusCode, target, body);
                yield new GitHubApiException("GitHub API error (" + statusCode + ") for: " + target, statusCode);
            }
        };
    }
}
