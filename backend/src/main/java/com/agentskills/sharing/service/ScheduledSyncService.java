package com.agentskills.sharing.service;

import com.agentskills.sharing.dto.GitHubRepoInfo;
import com.agentskills.sharing.entity.Repository;
import com.agentskills.sharing.security.EncryptionUtil;
import com.agentskills.sharing.repository.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled service that periodically syncs Star and Fork counts
 * for all imported repositories from the GitHub API.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledSyncService {

    private final RepositoryRepository repositoryRepository;
    private final GitHubApiClient gitHubApiClient;
    private final EncryptionUtil encryptionUtil;

    /**
     * Every 6 hours, batch sync Star_Count and Fork_Count for all repositories.
     */
    @Scheduled(cron = "0 0 */6 * * *")
    public void syncAllRepositoryStats() {
        List<Repository> repositories = repositoryRepository.findAll();
        log.info("Starting scheduled Star/Fork sync for {} repositories", repositories.size());

        int successCount = 0;
        int failCount = 0;

        for (Repository repo : repositories) {
            try {
                syncRepositoryStats(repo);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.warn("Failed to sync stats for {}/{}: {}",
                        repo.getGithubOwner(), repo.getGithubRepo(), e.getMessage());
            }
        }

        log.info("Scheduled sync completed: {} succeeded, {} failed out of {} total",
                successCount, failCount, repositories.size());
    }

    /**
     * Sync Star/Fork counts for a single repository.
     */
    void syncRepositoryStats(Repository repo) {
        String token = encryptionUtil.decrypt(repo.getUser().getAccessToken());
        GitHubRepoInfo repoInfo = gitHubApiClient.getRepoInfo(
                repo.getGithubOwner(), repo.getGithubRepo(), token);

        repo.setStarCount(repoInfo.stargazersCount());
        repo.setForkCount(repoInfo.forksCount());
        repo.setLastSyncedAt(LocalDateTime.now());
        repositoryRepository.save(repo);

        log.debug("Synced {}/{}: stars={}, forks={}",
                repo.getGithubOwner(), repo.getGithubRepo(),
                repoInfo.stargazersCount(), repoInfo.forksCount());
    }
}
