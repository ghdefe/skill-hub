package com.agentskills.sharing.service;

import com.agentskills.sharing.dto.GitHubRepoInfo;
import com.agentskills.sharing.entity.Repository;
import com.agentskills.sharing.entity.User;
import com.agentskills.sharing.repository.RepositoryRepository;
import com.agentskills.sharing.security.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledSyncServiceTest {

    @Mock
    private RepositoryRepository repositoryRepository;

    @Mock
    private GitHubApiClient gitHubApiClient;

    @Mock
    private EncryptionUtil encryptionUtil;

    private ScheduledSyncService scheduledSyncService;

    @BeforeEach
    void setUp() {
        scheduledSyncService = new ScheduledSyncService(
                repositoryRepository, gitHubApiClient, encryptionUtil);
    }

    @Test
    void syncAllRepositoryStats_withNoRepositories_shouldCompleteWithoutErrors() {
        when(repositoryRepository.findAll()).thenReturn(Collections.emptyList());

        scheduledSyncService.syncAllRepositoryStats();

        verify(repositoryRepository).findAll();
        verifyNoInteractions(gitHubApiClient);
    }

    @Test
    void syncAllRepositoryStats_withSingleRepository_shouldUpdateStarAndForkCounts() {
        Repository repo = createRepository("owner1", "repo1", "encrypted-token-1");
        when(repositoryRepository.findAll()).thenReturn(List.of(repo));
        when(encryptionUtil.decrypt("encrypted-token-1")).thenReturn("plain-token-1");
        when(gitHubApiClient.getRepoInfo("owner1", "repo1", "plain-token-1"))
                .thenReturn(new GitHubRepoInfo(42, 7));

        scheduledSyncService.syncAllRepositoryStats();

        ArgumentCaptor<Repository> captor = ArgumentCaptor.forClass(Repository.class);
        verify(repositoryRepository).save(captor.capture());
        Repository saved = captor.getValue();
        assertThat(saved.getStarCount()).isEqualTo(42);
        assertThat(saved.getForkCount()).isEqualTo(7);
        assertThat(saved.getLastSyncedAt()).isNotNull();
    }

    @Test
    void syncAllRepositoryStats_withMultipleRepositories_shouldSyncAll() {
        Repository repo1 = createRepository("owner1", "repo1", "enc-1");
        Repository repo2 = createRepository("owner2", "repo2", "enc-2");
        when(repositoryRepository.findAll()).thenReturn(List.of(repo1, repo2));
        when(encryptionUtil.decrypt("enc-1")).thenReturn("token-1");
        when(encryptionUtil.decrypt("enc-2")).thenReturn("token-2");
        when(gitHubApiClient.getRepoInfo("owner1", "repo1", "token-1"))
                .thenReturn(new GitHubRepoInfo(10, 2));
        when(gitHubApiClient.getRepoInfo("owner2", "repo2", "token-2"))
                .thenReturn(new GitHubRepoInfo(100, 50));

        scheduledSyncService.syncAllRepositoryStats();

        verify(repositoryRepository, times(2)).save(any(Repository.class));
        assertThat(repo1.getStarCount()).isEqualTo(10);
        assertThat(repo1.getForkCount()).isEqualTo(2);
        assertThat(repo2.getStarCount()).isEqualTo(100);
        assertThat(repo2.getForkCount()).isEqualTo(50);
    }

    @Test
    void syncAllRepositoryStats_whenOneRepoFails_shouldContinueWithOthers() {
        Repository repo1 = createRepository("owner1", "repo1", "enc-1");
        Repository repo2 = createRepository("owner2", "repo2", "enc-2");
        when(repositoryRepository.findAll()).thenReturn(List.of(repo1, repo2));
        when(encryptionUtil.decrypt("enc-1")).thenThrow(new RuntimeException("Decryption failed"));
        when(encryptionUtil.decrypt("enc-2")).thenReturn("token-2");
        when(gitHubApiClient.getRepoInfo("owner2", "repo2", "token-2"))
                .thenReturn(new GitHubRepoInfo(55, 11));

        scheduledSyncService.syncAllRepositoryStats();

        // repo1 failed but repo2 should still be saved
        verify(repositoryRepository, times(1)).save(any(Repository.class));
        assertThat(repo2.getStarCount()).isEqualTo(55);
        assertThat(repo2.getForkCount()).isEqualTo(11);
    }

    @Test
    void syncAllRepositoryStats_whenGitHubApiFails_shouldNotUpdateRepository() {
        Repository repo = createRepository("owner1", "repo1", "enc-1");
        when(repositoryRepository.findAll()).thenReturn(List.of(repo));
        when(encryptionUtil.decrypt("enc-1")).thenReturn("token-1");
        when(gitHubApiClient.getRepoInfo("owner1", "repo1", "token-1"))
                .thenThrow(new RuntimeException("GitHub API error"));

        scheduledSyncService.syncAllRepositoryStats();

        verify(repositoryRepository, never()).save(any(Repository.class));
        assertThat(repo.getStarCount()).isEqualTo(0);
        assertThat(repo.getForkCount()).isEqualTo(0);
    }

    @Test
    void syncRepositoryStats_shouldDecryptTokenAndCallGitHubApi() {
        Repository repo = createRepository("myOwner", "myRepo", "encrypted-access");
        when(encryptionUtil.decrypt("encrypted-access")).thenReturn("decrypted-access");
        when(gitHubApiClient.getRepoInfo("myOwner", "myRepo", "decrypted-access"))
                .thenReturn(new GitHubRepoInfo(200, 30));

        scheduledSyncService.syncRepositoryStats(repo);

        verify(encryptionUtil).decrypt("encrypted-access");
        verify(gitHubApiClient).getRepoInfo("myOwner", "myRepo", "decrypted-access");
        verify(repositoryRepository).save(repo);
        assertThat(repo.getStarCount()).isEqualTo(200);
        assertThat(repo.getForkCount()).isEqualTo(30);
        assertThat(repo.getLastSyncedAt()).isNotNull();
    }

    private Repository createRepository(String owner, String repoName, String encryptedToken) {
        User user = new User();
        user.setId("user-id-" + owner);
        user.setAccessToken(encryptedToken);

        Repository repo = new Repository();
        repo.setId("repo-id-" + repoName);
        repo.setGithubOwner(owner);
        repo.setGithubRepo(repoName);
        repo.setUser(user);
        repo.setStarCount(0);
        repo.setForkCount(0);
        return repo;
    }
}
