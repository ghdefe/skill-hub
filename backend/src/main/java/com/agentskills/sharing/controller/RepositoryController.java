package com.agentskills.sharing.controller;

import com.agentskills.sharing.dto.ImportRepositoryRequest;
import com.agentskills.sharing.dto.RepositoryResponse;
import com.agentskills.sharing.entity.Repository;
import com.agentskills.sharing.entity.SkillGroup;
import com.agentskills.sharing.entity.SkillStatus;
import com.agentskills.sharing.repository.RepositoryRepository;
import com.agentskills.sharing.repository.SkillGroupRepository;
import com.agentskills.sharing.repository.SkillRepository;
import com.agentskills.sharing.service.RepositoryScannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agentskills.sharing.dto.GitHubRepoInfo;
import com.agentskills.sharing.repository.UserRepository;
import com.agentskills.sharing.security.EncryptionUtil;
import com.agentskills.sharing.service.GitHubApiClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * REST API controller for GitHub repository import and management.
 *
 * <ul>
 *   <li>POST /api/repositories - Import a GitHub repository</li>
 *   <li>GET /api/repositories - List current user's repositories</li>
 *   <li>DELETE /api/repositories/{id} - Remove a repository</li>
 *   <li>POST /api/repositories/{id}/sync - Manually sync a repository</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/repositories")
@RequiredArgsConstructor
@Slf4j
public class RepositoryController {

    private final RepositoryScannerService repositoryScannerService;
    private final RepositoryRepository repositoryRepository;
    private final SkillGroupRepository skillGroupRepository;
    private final SkillRepository skillRepository;
    private final GitHubApiClient gitHubApiClient;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;

    /**
     * Import a GitHub repository by scanning its skills/ directory.
     */
    @PostMapping
    public ResponseEntity<RepositoryResponse> importRepository(
            @Valid @RequestBody ImportRepositoryRequest request,
            Authentication authentication) {

        String userId = (String) authentication.getPrincipal();
        Repository repo = repositoryScannerService.importRepository(
                request.url(), userId, request.effectiveScanPath());

        RepositoryResponse response = buildResponse(repo);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List all repositories for the current authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<RepositoryResponse>> listRepositories(Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        List<Repository> repos = repositoryRepository.findByUserId(userId);

        List<RepositoryResponse> responses = repos.stream()
                .map(this::buildResponse)
                .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Remove a repository and its associated SkillGroup and Skills (cascade).
     * Verifies the repository belongs to the current user.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRepository(
            @PathVariable String id,
            Authentication authentication) {

        String userId = (String) authentication.getPrincipal();
        Repository repo = repositoryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("仓库不存在"));

        if (!repo.getUser().getId().equals(userId)) {
            throw new SecurityException("无权操作此仓库");
        }

        repositoryRepository.delete(repo);
        return ResponseEntity.noContent().build();
    }

    /**
     * Manually sync a repository: re-scan skills and update Star/Fork counts.
     * Only the repository owner can trigger a sync.
     */
    @PostMapping("/{id}/sync")
    public ResponseEntity<RepositoryResponse> syncRepository(
            @PathVariable String id,
            Authentication authentication) {

        String userId = (String) authentication.getPrincipal();
        Repository repo = repositoryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("仓库不存在"));

        if (!repo.getUser().getId().equals(userId)) {
            throw new SecurityException("无权操作此仓库");
        }

        // Re-scan using stored scanPath
        Repository updatedRepo = repositoryScannerService.importRepository(
                repo.getUrl(), userId, repo.getScanPath());

        // Update Star/Fork counts from GitHub API
        try {
            var user = userRepository.findById(userId)
                    .orElseThrow(() -> new NoSuchElementException("用户不存在"));
            String token = encryptionUtil.decrypt(user.getAccessToken());
            GitHubRepoInfo repoInfo = gitHubApiClient.getRepoInfo(
                    updatedRepo.getGithubOwner(), updatedRepo.getGithubRepo(), token);
            updatedRepo.setStarCount(repoInfo.stargazersCount());
            updatedRepo.setForkCount(repoInfo.forksCount());
            updatedRepo.setLastSyncedAt(LocalDateTime.now());
            updatedRepo = repositoryRepository.save(updatedRepo);
        } catch (Exception e) {
            // Star/Fork update failure should not fail the entire sync
            log.warn("Failed to update Star/Fork counts for {}/{}: {}",
                    updatedRepo.getGithubOwner(), updatedRepo.getGithubRepo(), e.getMessage());
        }

        RepositoryResponse response = buildResponse(updatedRepo);
        return ResponseEntity.ok(response);
    }

    private RepositoryResponse buildResponse(Repository repo) {
        SkillGroup group = skillGroupRepository.findByRepositoryId(repo.getId()).orElse(null);
        int skillCount = 0;
        if (group != null) {
            skillCount = skillRepository.findBySkillGroupIdAndStatus(
                    group.getId(), SkillStatus.ACTIVE).size();
        }
        return RepositoryResponse.from(repo, group, skillCount);
    }
}
