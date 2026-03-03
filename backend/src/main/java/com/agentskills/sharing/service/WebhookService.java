package com.agentskills.sharing.service;

import com.agentskills.sharing.entity.Repository;
import com.agentskills.sharing.repository.RepositoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * Service for handling GitHub webhook events.
 * Validates HMAC-SHA256 signatures and processes push events
 * to detect changes in skills/ directory.
 */
@Service
@Slf4j
public class WebhookService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";
    private static final String SKILLS_DIR_PREFIX = "skills/";

    private final String webhookSecret;
    private final RepositoryRepository repositoryRepository;
    private final RepositoryScannerService repositoryScannerService;
    private final ObjectMapper objectMapper;

    public WebhookService(
            @Value("${github.api.webhook-secret}") String webhookSecret,
            RepositoryRepository repositoryRepository,
            RepositoryScannerService repositoryScannerService,
            ObjectMapper objectMapper) {
        this.webhookSecret = webhookSecret;
        this.repositoryRepository = repositoryRepository;
        this.repositoryScannerService = repositoryScannerService;
        this.objectMapper = objectMapper;
    }

    /**
     * Verify the X-Hub-Signature-256 header using HMAC-SHA256 with constant-time comparison.
     *
     * @param payload   the raw request body bytes
     * @param signature the X-Hub-Signature-256 header value (e.g., "sha256=abc123...")
     * @return true if the signature is valid
     */
    public boolean verifySignature(byte[] payload, String signature) {
        if (signature == null || !signature.startsWith(SIGNATURE_PREFIX)) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec keySpec = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(keySpec);
            byte[] computedHash = mac.doFinal(payload);
            String computedSignature = HexFormat.of().formatHex(computedHash);

            String providedSignature = signature.substring(SIGNATURE_PREFIX.length());

            // Constant-time comparison to prevent timing attacks
            return MessageDigest.isEqual(
                    computedSignature.getBytes(StandardCharsets.UTF_8),
                    providedSignature.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to compute HMAC-SHA256 signature", e);
            return false;
        }
    }

    /**
     * Process a GitHub push event payload.
     * Checks if any commits contain changes to the skills/ directory,
     * and if so, triggers a re-scan of the matching repository.
     *
     * @param payload the raw JSON payload as bytes
     */
    public void processPushEvent(byte[] payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);

            if (!containsSkillsChanges(root)) {
                log.debug("Push event does not contain skills/ changes, skipping");
                return;
            }

            // Extract repository URL from payload
            JsonNode repoNode = root.path("repository");
            String repoUrl = repoNode.path("html_url").asText(null);
            String owner = repoNode.path("owner").path("login").asText(
                    repoNode.path("owner").path("name").asText(null));
            String repoName = repoNode.path("name").asText(null);

            if (repoUrl == null || owner == null || repoName == null) {
                log.warn("Push event missing repository information");
                return;
            }

            // Find matching repositories and trigger re-scan
            List<Repository> matchingRepos = repositoryRepository.findByGithubOwnerAndGithubRepo(owner, repoName);

            if (matchingRepos.isEmpty()) {
                log.info("No matching repository found for {}/{}", owner, repoName);
                return;
            }

            for (Repository repo : matchingRepos) {
                try {
                    log.info("Triggering re-scan for repository {}/{} (user: {})",
                            owner, repoName, repo.getUser().getId());
                    repositoryScannerService.importRepository(repo.getUrl(), repo.getUser().getId(), repo.getScanPath(), repo.getScanBranch());
                } catch (Exception e) {
                    log.error("Failed to re-scan repository {}/{} for user {}: {}",
                            owner, repoName, repo.getUser().getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to process push event payload", e);
        }
    }

    /**
     * Check if any commits in the push event contain changes to the skills/ directory.
     */
    boolean containsSkillsChanges(JsonNode root) {
        JsonNode commits = root.path("commits");
        if (!commits.isArray()) {
            return false;
        }

        for (JsonNode commit : commits) {
            if (hasSkillsPath(commit.path("added"))
                    || hasSkillsPath(commit.path("modified"))
                    || hasSkillsPath(commit.path("removed"))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasSkillsPath(JsonNode files) {
        if (!files.isArray()) {
            return false;
        }
        for (JsonNode file : files) {
            if (file.asText("").startsWith(SKILLS_DIR_PREFIX)) {
                return true;
            }
        }
        return false;
    }
}
