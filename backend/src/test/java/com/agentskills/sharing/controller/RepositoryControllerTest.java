package com.agentskills.sharing.controller;

import com.agentskills.sharing.dto.GitHubRepoInfo;
import com.agentskills.sharing.dto.ImportRepositoryRequest;
import com.agentskills.sharing.dto.RepositoryResponse;
import com.agentskills.sharing.entity.Repository;
import com.agentskills.sharing.entity.Skill;
import com.agentskills.sharing.entity.SkillGroup;
import com.agentskills.sharing.entity.SkillStatus;
import com.agentskills.sharing.entity.User;
import com.agentskills.sharing.repository.RepositoryRepository;
import com.agentskills.sharing.repository.SkillGroupRepository;
import com.agentskills.sharing.repository.SkillRepository;
import com.agentskills.sharing.repository.UserRepository;
import com.agentskills.sharing.security.EncryptionUtil;
import com.agentskills.sharing.service.GitHubApiClient;
import com.agentskills.sharing.service.RepositoryScannerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryControllerTest {

    @Mock
    private RepositoryScannerService repositoryScannerService;

    @Mock
    private RepositoryRepository repositoryRepository;

    @Mock
    private SkillGroupRepository skillGroupRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private GitHubApiClient gitHubApiClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EncryptionUtil encryptionUtil;

    @InjectMocks
    private RepositoryController repositoryController;

    private Authentication authentication;
    private User testUser;
    private Repository testRepo;
    private SkillGroup testGroup;

    @BeforeEach
    void setUp() {
        authentication = new UsernamePasswordAuthenticationToken(
                "user-123", null, Collections.emptyList());

        testUser = new User();
        testUser.setId("user-123");
        testUser.setUsername("testuser");

        testRepo = new Repository();
        testRepo.setId("repo-1");
        testRepo.setUser(testUser);
        testRepo.setGithubOwner("owner");
        testRepo.setGithubRepo("my-repo");
        testRepo.setUrl("https://github.com/owner/my-repo");
        testRepo.setStarCount(42);
        testRepo.setForkCount(5);
        testRepo.setLastSyncedAt(LocalDateTime.now());
        testRepo.setCreatedAt(LocalDateTime.now());

        testGroup = new SkillGroup();
        testGroup.setId("group-1");
        testGroup.setName("my-repo");
        testGroup.setRepository(testRepo);
        testGroup.setUser(testUser);
    }

    @Test
    void importRepository_shouldReturn201WithRepositoryResponse() {
        var request = new ImportRepositoryRequest("https://github.com/owner/my-repo");
        when(repositoryScannerService.importRepository("https://github.com/owner/my-repo", "user-123"))
                .thenReturn(testRepo);
        when(skillGroupRepository.findByRepositoryId("repo-1"))
                .thenReturn(Optional.of(testGroup));
        when(skillRepository.findBySkillGroupIdAndStatus("group-1", SkillStatus.ACTIVE))
                .thenReturn(List.of(createSkill("skill-1"), createSkill("skill-2")));

        var result = repositoryController.importRepository(request, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        RepositoryResponse body = result.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo("repo-1");
        assertThat(body.githubOwner()).isEqualTo("owner");
        assertThat(body.githubRepo()).isEqualTo("my-repo");
        assertThat(body.url()).isEqualTo("https://github.com/owner/my-repo");
        assertThat(body.starCount()).isEqualTo(42);
        assertThat(body.forkCount()).isEqualTo(5);
        assertThat(body.skillGroup()).isNotNull();
        assertThat(body.skillGroup().name()).isEqualTo("my-repo");
        assertThat(body.skillGroup().skillCount()).isEqualTo(2);
    }

    @Test
    void importRepository_withNoSkillGroup_shouldReturnNullSkillGroup() {
        var request = new ImportRepositoryRequest("https://github.com/owner/my-repo");
        when(repositoryScannerService.importRepository("https://github.com/owner/my-repo", "user-123"))
                .thenReturn(testRepo);
        when(skillGroupRepository.findByRepositoryId("repo-1"))
                .thenReturn(Optional.empty());

        var result = repositoryController.importRepository(request, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().skillGroup()).isNull();
    }

    @Test
    void listRepositories_shouldReturnUserRepositories() {
        when(repositoryRepository.findByUserId("user-123"))
                .thenReturn(List.of(testRepo));
        when(skillGroupRepository.findByRepositoryId("repo-1"))
                .thenReturn(Optional.of(testGroup));
        when(skillRepository.findBySkillGroupIdAndStatus("group-1", SkillStatus.ACTIVE))
                .thenReturn(List.of(createSkill("skill-1")));

        var result = repositoryController.listRepositories(authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<RepositoryResponse> body = result.getBody();
        assertThat(body).isNotNull().hasSize(1);
        assertThat(body.get(0).id()).isEqualTo("repo-1");
        assertThat(body.get(0).skillGroup().skillCount()).isEqualTo(1);
    }

    @Test
    void listRepositories_withNoRepos_shouldReturnEmptyList() {
        when(repositoryRepository.findByUserId("user-123"))
                .thenReturn(Collections.emptyList());

        var result = repositoryController.listRepositories(authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull().isEmpty();
    }

    @Test
    void deleteRepository_shouldReturn204() {
        when(repositoryRepository.findById("repo-1"))
                .thenReturn(Optional.of(testRepo));

        var result = repositoryController.deleteRepository("repo-1", authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(repositoryRepository).delete(testRepo);
    }

    @Test
    void deleteRepository_withNonExistentRepo_shouldThrowNotFound() {
        when(repositoryRepository.findById("non-existent"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> repositoryController.deleteRepository("non-existent", authentication))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("仓库不存在");
    }

    @Test
    void deleteRepository_withDifferentUser_shouldThrowSecurityException() {
        User otherUser = new User();
        otherUser.setId("other-user");

        Repository otherRepo = new Repository();
        otherRepo.setId("repo-2");
        otherRepo.setUser(otherUser);

        when(repositoryRepository.findById("repo-2"))
                .thenReturn(Optional.of(otherRepo));

        assertThatThrownBy(() -> repositoryController.deleteRepository("repo-2", authentication))
                .isInstanceOf(SecurityException.class)
                .hasMessage("无权操作此仓库");
    }

    @Test
    void syncRepository_shouldReturn200WithUpdatedResponse() {
        when(repositoryRepository.findById("repo-1"))
                .thenReturn(Optional.of(testRepo));
        when(repositoryScannerService.importRepository("https://github.com/owner/my-repo", "user-123"))
                .thenReturn(testRepo);
        when(userRepository.findById("user-123"))
                .thenReturn(Optional.of(testUser));
        when(encryptionUtil.decrypt(null))
                .thenReturn("fake-token");
        when(gitHubApiClient.getRepoInfo("owner", "my-repo", "fake-token"))
                .thenReturn(new GitHubRepoInfo(100, 20));
        when(repositoryRepository.save(testRepo))
                .thenReturn(testRepo);
        when(skillGroupRepository.findByRepositoryId("repo-1"))
                .thenReturn(Optional.of(testGroup));
        when(skillRepository.findBySkillGroupIdAndStatus("group-1", SkillStatus.ACTIVE))
                .thenReturn(List.of(createSkill("skill-1")));

        var result = repositoryController.syncRepository("repo-1", authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        RepositoryResponse body = result.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isEqualTo("repo-1");
        assertThat(body.githubOwner()).isEqualTo("owner");
        assertThat(body.githubRepo()).isEqualTo("my-repo");
        verify(repositoryScannerService).importRepository("https://github.com/owner/my-repo", "user-123");
        verify(gitHubApiClient).getRepoInfo("owner", "my-repo", "fake-token");
    }

    @Test
    void syncRepository_withNonExistentRepo_shouldThrowNotFound() {
        when(repositoryRepository.findById("non-existent"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> repositoryController.syncRepository("non-existent", authentication))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("仓库不存在");
    }

    @Test
    void syncRepository_withDifferentUser_shouldThrowSecurityException() {
        User otherUser = new User();
        otherUser.setId("other-user");

        Repository otherRepo = new Repository();
        otherRepo.setId("repo-2");
        otherRepo.setUser(otherUser);

        when(repositoryRepository.findById("repo-2"))
                .thenReturn(Optional.of(otherRepo));

        assertThatThrownBy(() -> repositoryController.syncRepository("repo-2", authentication))
                .isInstanceOf(SecurityException.class)
                .hasMessage("无权操作此仓库");
    }

    @Test
    void syncRepository_whenStarForkUpdateFails_shouldStillReturnOk() {
        when(repositoryRepository.findById("repo-1"))
                .thenReturn(Optional.of(testRepo));
        when(repositoryScannerService.importRepository("https://github.com/owner/my-repo", "user-123"))
                .thenReturn(testRepo);
        when(userRepository.findById("user-123"))
                .thenReturn(Optional.of(testUser));
        when(encryptionUtil.decrypt(null))
                .thenReturn("fake-token");
        when(gitHubApiClient.getRepoInfo("owner", "my-repo", "fake-token"))
                .thenThrow(new RuntimeException("GitHub API error"));
        when(skillGroupRepository.findByRepositoryId("repo-1"))
                .thenReturn(Optional.of(testGroup));
        when(skillRepository.findBySkillGroupIdAndStatus("group-1", SkillStatus.ACTIVE))
                .thenReturn(List.of(createSkill("skill-1")));

        var result = repositoryController.syncRepository("repo-1", authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        // Sync should succeed even if Star/Fork update fails
        verify(repositoryScannerService).importRepository("https://github.com/owner/my-repo", "user-123");
    }

    private Skill createSkill(String id) {
        Skill skill = new Skill();
        skill.setId(id);
        skill.setName("skill-" + id);
        skill.setFolderPath("skills/skill-" + id);
        skill.setStatus(SkillStatus.ACTIVE);
        return skill;
    }
}
