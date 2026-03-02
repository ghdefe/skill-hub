package com.agentskills.sharing.service;

import com.agentskills.sharing.dto.GitHubContentItem;
import com.agentskills.sharing.dto.GitHubRepoInfo;
import com.agentskills.sharing.entity.Repository;
import com.agentskills.sharing.entity.Skill;
import com.agentskills.sharing.entity.SkillGroup;
import com.agentskills.sharing.entity.SkillStatus;
import com.agentskills.sharing.entity.Tag;
import com.agentskills.sharing.entity.User;
import com.agentskills.sharing.exception.GitHubApiException;
import com.agentskills.sharing.repository.RepositoryRepository;
import com.agentskills.sharing.repository.SkillGroupRepository;
import com.agentskills.sharing.repository.SkillRepository;
import com.agentskills.sharing.repository.TagRepository;
import com.agentskills.sharing.repository.UserRepository;
import com.agentskills.sharing.security.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.argThat;

@ExtendWith(MockitoExtension.class)
class RepositoryScannerServiceTest {

    @Mock
    private GitHubApiClient gitHubApiClient;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RepositoryRepository repositoryRepository;
    @Mock
    private SkillGroupRepository skillGroupRepository;
    @Mock
    private SkillRepository skillRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private EncryptionUtil encryptionUtil;

    @InjectMocks
    private RepositoryScannerService service;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("user-1");
        testUser.setGithubId("12345");
        testUser.setUsername("testuser");
        testUser.setAccessToken("encrypted-token");
    }

    // --- URL Validation Tests ---

    @Test
    void importRepository_invalidUrl_throwsIllegalArgument() {
        assertThatThrownBy(() -> service.importRepository("not-a-url", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("仓库 URL 格式无效");
    }

    @Test
    void importRepository_missingOwnerOrRepo_throwsIllegalArgument() {
        assertThatThrownBy(() -> service.importRepository("https://github.com/owner", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("仓库 URL 格式无效");
    }

    @Test
    void importRepository_nonGithubUrl_throwsIllegalArgument() {
        assertThatThrownBy(() -> service.importRepository("https://gitlab.com/owner/repo", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("仓库 URL 格式无效");
    }

    @Test
    void importRepository_urlWithExtraPath_throwsIllegalArgument() {
        assertThatThrownBy(() -> service.importRepository("https://github.com/owner/repo/tree/main", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("仓库 URL 格式无效");
    }

    // --- Skills Directory Error Tests ---

    @Test
    void importRepository_noSkillsDirectory_throwsIllegalArgument() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(encryptionUtil.decrypt("encrypted-token")).thenReturn("real-token");
        when(gitHubApiClient.getRepoContents("owner", "repo", "skills", "real-token"))
                .thenThrow(new GitHubApiException("Not found", 404));

        assertThatThrownBy(() -> service.importRepository("https://github.com/owner/repo", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("未找到 skills/ 目录，请确认仓库结构");
    }

    @Test
    void importRepository_repoNotAccessible_throwsIllegalArgument() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(encryptionUtil.decrypt("encrypted-token")).thenReturn("real-token");
        when(gitHubApiClient.getRepoContents("owner", "repo", "skills", "real-token"))
                .thenThrow(new GitHubApiException("Forbidden", 403));

        assertThatThrownBy(() -> service.importRepository("https://github.com/owner/repo", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("仓库不可访问，请检查 URL 和权限");
    }

    @Test
    void importRepository_emptySkillsDirectory_throwsIllegalArgument() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(encryptionUtil.decrypt("encrypted-token")).thenReturn("real-token");
        // Return only files, no directories
        when(gitHubApiClient.getRepoContents("owner", "repo", "skills", "real-token"))
                .thenReturn(List.of(
                        new GitHubContentItem("README.md", "file", "skills/README.md", "https://example.com")
                ));

        assertThatThrownBy(() -> service.importRepository("https://github.com/owner/repo", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("skills/ 目录下未找到任何 Skill，请确认目录结构");
    }

    // --- Successful Import Tests ---

    @Test
    void importRepository_withManifestJson_createsSkillWithManifestData() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(encryptionUtil.decrypt("encrypted-token")).thenReturn("real-token");

        // skills/ directory has one subfolder
        when(gitHubApiClient.getRepoContents("owner", "my-repo", "skills", "real-token"))
                .thenReturn(List.of(
                        new GitHubContentItem("my-skill", "dir", "skills/my-skill", null)
                ));

        // manifest.json exists
        String manifestJson = """
                {"name": "My Awesome Skill", "description": "A great skill", "tags": ["nlp", "chat"]}
                """;
        when(gitHubApiClient.getFileContent("owner", "my-repo", "skills/my-skill/manifest.json", "real-token"))
                .thenReturn(manifestJson);

        // README.md exists
        when(gitHubApiClient.getFileContent("owner", "my-repo", "skills/my-skill/README.md", "real-token"))
                .thenReturn("# My Awesome Skill\nDetailed description here.");

        // Repo info
        when(gitHubApiClient.getRepoInfo("owner", "my-repo", "real-token"))
                .thenReturn(new GitHubRepoInfo(42, 7));

        // No existing repo/group/skill
        when(repositoryRepository.findByUserIdAndGithubOwnerAndGithubRepo("user-1", "owner", "my-repo"))
                .thenReturn(Optional.empty());
        when(repositoryRepository.save(any(Repository.class))).thenAnswer(inv -> {
            Repository r = inv.getArgument(0);
            if (r.getId() == null) r.setId("repo-1");
            return r;
        });
        when(skillGroupRepository.findByRepositoryId("repo-1")).thenReturn(Optional.empty());
        when(skillGroupRepository.save(any(SkillGroup.class))).thenAnswer(inv -> {
            SkillGroup sg = inv.getArgument(0);
            if (sg.getId() == null) sg.setId("group-1");
            return sg;
        });
        when(skillRepository.findByUserIdAndName("user-1", "My Awesome Skill")).thenReturn(Optional.empty());
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));

        // Tags
        when(tagRepository.findByName("nlp")).thenReturn(Optional.empty());
        when(tagRepository.findByName("chat")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(inv -> {
            Tag t = inv.getArgument(0);
            if (t.getId() == null) t.setId("tag-" + t.getName());
            return t;
        });

        Repository result = service.importRepository("https://github.com/owner/my-repo", "user-1");

        assertThat(result).isNotNull();
        assertThat(result.getGithubOwner()).isEqualTo("owner");
        assertThat(result.getGithubRepo()).isEqualTo("my-repo");
        assertThat(result.getStarCount()).isEqualTo(42);
        assertThat(result.getForkCount()).isEqualTo(7);

        verify(skillRepository).save(any(Skill.class));
        // Two tags: "nlp" and "chat"
        verify(tagRepository, org.mockito.Mockito.times(2)).save(any(Tag.class));
    }

    @Test
    void importRepository_withoutManifest_usesFolderNameAndReadmeFirstLine() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(encryptionUtil.decrypt("encrypted-token")).thenReturn("real-token");

        when(gitHubApiClient.getRepoContents("owner", "repo", "skills", "real-token"))
                .thenReturn(List.of(
                        new GitHubContentItem("simple-skill", "dir", "skills/simple-skill", null)
                ));

        // No manifest.json
        when(gitHubApiClient.getFileContent("owner", "repo", "skills/simple-skill/manifest.json", "real-token"))
                .thenThrow(new GitHubApiException("Not found", 404));
        // No manifest.yaml
        when(gitHubApiClient.getFileContent("owner", "repo", "skills/simple-skill/manifest.yaml", "real-token"))
                .thenThrow(new GitHubApiException("Not found", 404));

        // README.md exists
        when(gitHubApiClient.getFileContent("owner", "repo", "skills/simple-skill/README.md", "real-token"))
                .thenReturn("# Simple Skill\nThis is a simple skill.");

        when(gitHubApiClient.getRepoInfo("owner", "repo", "real-token"))
                .thenReturn(new GitHubRepoInfo(10, 2));

        when(repositoryRepository.findByUserIdAndGithubOwnerAndGithubRepo("user-1", "owner", "repo"))
                .thenReturn(Optional.empty());
        when(repositoryRepository.save(any(Repository.class))).thenAnswer(inv -> {
            Repository r = inv.getArgument(0);
            if (r.getId() == null) r.setId("repo-1");
            return r;
        });
        when(skillGroupRepository.findByRepositoryId("repo-1")).thenReturn(Optional.empty());
        when(skillGroupRepository.save(any(SkillGroup.class))).thenAnswer(inv -> {
            SkillGroup sg = inv.getArgument(0);
            if (sg.getId() == null) sg.setId("group-1");
            return sg;
        });
        // Skill lookup by folder name (no manifest, so name = folder name)
        when(skillRepository.findByUserIdAndName("user-1", "simple-skill")).thenReturn(Optional.empty());
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));

        Repository result = service.importRepository("https://github.com/owner/repo", "user-1");

        assertThat(result).isNotNull();
        verify(skillRepository).save(any(Skill.class));
        // No tags should be saved since no manifest
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void importRepository_validUrlWithTrailingSlash_succeeds() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(encryptionUtil.decrypt("encrypted-token")).thenReturn("real-token");

        when(gitHubApiClient.getRepoContents("owner", "repo", "skills", "real-token"))
                .thenReturn(List.of(
                        new GitHubContentItem("skill-a", "dir", "skills/skill-a", null)
                ));

        when(gitHubApiClient.getFileContent(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new GitHubApiException("Not found", 404));

        when(gitHubApiClient.getRepoInfo("owner", "repo", "real-token"))
                .thenReturn(new GitHubRepoInfo(0, 0));

        when(repositoryRepository.findByUserIdAndGithubOwnerAndGithubRepo("user-1", "owner", "repo"))
                .thenReturn(Optional.empty());
        when(repositoryRepository.save(any(Repository.class))).thenAnswer(inv -> {
            Repository r = inv.getArgument(0);
            if (r.getId() == null) r.setId("repo-1");
            return r;
        });
        when(skillGroupRepository.findByRepositoryId("repo-1")).thenReturn(Optional.empty());
        when(skillGroupRepository.save(any(SkillGroup.class))).thenAnswer(inv -> {
            SkillGroup sg = inv.getArgument(0);
            if (sg.getId() == null) sg.setId("group-1");
            return sg;
        });
        when(skillRepository.findByUserIdAndName(eq("user-1"), anyString())).thenReturn(Optional.empty());
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));

        // URL with trailing slash should work
        Repository result = service.importRepository("https://github.com/owner/repo/", "user-1");
        assertThat(result).isNotNull();
    }

    @Test
    void importRepository_multipleSkillFolders_createsMultipleSkills() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(encryptionUtil.decrypt("encrypted-token")).thenReturn("real-token");

        when(gitHubApiClient.getRepoContents("owner", "repo", "skills", "real-token"))
                .thenReturn(List.of(
                        new GitHubContentItem("skill-a", "dir", "skills/skill-a", null),
                        new GitHubContentItem("skill-b", "dir", "skills/skill-b", null),
                        new GitHubContentItem("README.md", "file", "skills/README.md", "https://example.com")
                ));

        // All file reads return 404 (no manifests, no READMEs)
        when(gitHubApiClient.getFileContent(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new GitHubApiException("Not found", 404));

        when(gitHubApiClient.getRepoInfo("owner", "repo", "real-token"))
                .thenReturn(new GitHubRepoInfo(5, 1));

        when(repositoryRepository.findByUserIdAndGithubOwnerAndGithubRepo("user-1", "owner", "repo"))
                .thenReturn(Optional.empty());
        when(repositoryRepository.save(any(Repository.class))).thenAnswer(inv -> {
            Repository r = inv.getArgument(0);
            if (r.getId() == null) r.setId("repo-1");
            return r;
        });
        when(skillGroupRepository.findByRepositoryId("repo-1")).thenReturn(Optional.empty());
        when(skillGroupRepository.save(any(SkillGroup.class))).thenAnswer(inv -> {
            SkillGroup sg = inv.getArgument(0);
            if (sg.getId() == null) sg.setId("group-1");
            return sg;
        });
        when(skillRepository.findByUserIdAndName(eq("user-1"), anyString())).thenReturn(Optional.empty());
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));

        service.importRepository("https://github.com/owner/repo", "user-1");

        // Should save exactly 2 skills (directories only, not the README.md file)
        verify(skillRepository, times(2)).save(any(Skill.class));
    }

    // --- Re-import Diff Detection Tests ---

    @Test
    void reimport_removedSkillFolder_marksSkillAsRemoved() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(encryptionUtil.decrypt("encrypted-token")).thenReturn("real-token");

        // New scan only has skill-b (skill-a was removed from the repo)
        when(gitHubApiClient.getRepoContents("owner", "repo", "skills", "real-token"))
                .thenReturn(List.of(
                        new GitHubContentItem("skill-b", "dir", "skills/skill-b", null)
                ));
        when(gitHubApiClient.getFileContent(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new GitHubApiException("Not found", 404));
        when(gitHubApiClient.getRepoInfo("owner", "repo", "real-token"))
                .thenReturn(new GitHubRepoInfo(5, 1));

        // Existing repo and group
        Repository existingRepo = new Repository();
        existingRepo.setId("repo-1");
        existingRepo.setUser(testUser);
        existingRepo.setGithubOwner("owner");
        existingRepo.setGithubRepo("repo");
        existingRepo.setUrl("https://github.com/owner/repo");
        when(repositoryRepository.findByUserIdAndGithubOwnerAndGithubRepo("user-1", "owner", "repo"))
                .thenReturn(Optional.of(existingRepo));
        when(repositoryRepository.save(any(Repository.class))).thenReturn(existingRepo);

        SkillGroup existingGroup = new SkillGroup();
        existingGroup.setId("group-1");
        existingGroup.setRepository(existingRepo);
        existingGroup.setUser(testUser);
        existingGroup.setName("repo");
        when(skillGroupRepository.findByRepositoryId("repo-1")).thenReturn(Optional.of(existingGroup));
        when(skillGroupRepository.save(any(SkillGroup.class))).thenReturn(existingGroup);

        // Existing skills in the group: skill-a and skill-b
        Skill existingSkillA = new Skill();
        existingSkillA.setId("skill-a-id");
        existingSkillA.setName("skill-a");
        existingSkillA.setFolderPath("skills/skill-a");
        existingSkillA.setStatus(SkillStatus.ACTIVE);
        existingSkillA.setUser(testUser);
        existingSkillA.setSkillGroup(existingGroup);

        Skill existingSkillB = new Skill();
        existingSkillB.setId("skill-b-id");
        existingSkillB.setName("skill-b");
        existingSkillB.setFolderPath("skills/skill-b");
        existingSkillB.setStatus(SkillStatus.ACTIVE);
        existingSkillB.setUser(testUser);
        existingSkillB.setSkillGroup(existingGroup);

        when(skillRepository.findBySkillGroupId("group-1"))
                .thenReturn(List.of(existingSkillA, existingSkillB));

        // skill-b is found by name (update path)
        when(skillRepository.findByUserIdAndName("user-1", "skill-b"))
                .thenReturn(Optional.of(existingSkillB));

        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));

        service.importRepository("https://github.com/owner/repo", "user-1");

        // skill-a should be marked as REMOVED
        verify(skillRepository).save(argThat(skill ->
                "skill-a-id".equals(skill.getId()) && skill.getStatus() == SkillStatus.REMOVED));
        // skill-b should remain ACTIVE
        verify(skillRepository).save(argThat(skill ->
                "skill-b-id".equals(skill.getId()) && skill.getStatus() == SkillStatus.ACTIVE));
    }

    @Test
    void reimport_newSkillFolder_createsNewActiveSkill() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(encryptionUtil.decrypt("encrypted-token")).thenReturn("real-token");

        // New scan has skill-a (existing) and skill-c (new)
        when(gitHubApiClient.getRepoContents("owner", "repo", "skills", "real-token"))
                .thenReturn(List.of(
                        new GitHubContentItem("skill-a", "dir", "skills/skill-a", null),
                        new GitHubContentItem("skill-c", "dir", "skills/skill-c", null)
                ));
        when(gitHubApiClient.getFileContent(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new GitHubApiException("Not found", 404));
        when(gitHubApiClient.getRepoInfo("owner", "repo", "real-token"))
                .thenReturn(new GitHubRepoInfo(5, 1));

        Repository existingRepo = new Repository();
        existingRepo.setId("repo-1");
        existingRepo.setUser(testUser);
        existingRepo.setGithubOwner("owner");
        existingRepo.setGithubRepo("repo");
        existingRepo.setUrl("https://github.com/owner/repo");
        when(repositoryRepository.findByUserIdAndGithubOwnerAndGithubRepo("user-1", "owner", "repo"))
                .thenReturn(Optional.of(existingRepo));
        when(repositoryRepository.save(any(Repository.class))).thenReturn(existingRepo);

        SkillGroup existingGroup = new SkillGroup();
        existingGroup.setId("group-1");
        existingGroup.setRepository(existingRepo);
        existingGroup.setUser(testUser);
        existingGroup.setName("repo");
        when(skillGroupRepository.findByRepositoryId("repo-1")).thenReturn(Optional.of(existingGroup));
        when(skillGroupRepository.save(any(SkillGroup.class))).thenReturn(existingGroup);

        // Only skill-a exists in the group
        Skill existingSkillA = new Skill();
        existingSkillA.setId("skill-a-id");
        existingSkillA.setName("skill-a");
        existingSkillA.setFolderPath("skills/skill-a");
        existingSkillA.setStatus(SkillStatus.ACTIVE);
        existingSkillA.setUser(testUser);
        existingSkillA.setSkillGroup(existingGroup);

        when(skillRepository.findBySkillGroupId("group-1"))
                .thenReturn(List.of(existingSkillA));

        when(skillRepository.findByUserIdAndName("user-1", "skill-a"))
                .thenReturn(Optional.of(existingSkillA));
        when(skillRepository.findByUserIdAndName("user-1", "skill-c"))
                .thenReturn(Optional.empty());

        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));

        service.importRepository("https://github.com/owner/repo", "user-1");

        // skill-a updated, skill-c created as new ACTIVE
        verify(skillRepository, times(2)).save(argThat(skill ->
                skill.getStatus() == SkillStatus.ACTIVE));
        // No skill should be marked REMOVED
        verify(skillRepository, never()).save(argThat(skill ->
                skill.getStatus() == SkillStatus.REMOVED));
    }

    @Test
    void reimport_skillNameConflictAcrossGroups_throwsIllegalArgument() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(encryptionUtil.decrypt("encrypted-token")).thenReturn("real-token");

        when(gitHubApiClient.getRepoContents("owner", "repo2", "skills", "real-token"))
                .thenReturn(List.of(
                        new GitHubContentItem("conflicting-skill", "dir", "skills/conflicting-skill", null)
                ));
        when(gitHubApiClient.getFileContent(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new GitHubApiException("Not found", 404));
        when(gitHubApiClient.getRepoInfo("owner", "repo2", "real-token"))
                .thenReturn(new GitHubRepoInfo(0, 0));

        Repository repo2 = new Repository();
        repo2.setId("repo-2");
        repo2.setUser(testUser);
        repo2.setGithubOwner("owner");
        repo2.setGithubRepo("repo2");
        repo2.setUrl("https://github.com/owner/repo2");
        when(repositoryRepository.findByUserIdAndGithubOwnerAndGithubRepo("user-1", "owner", "repo2"))
                .thenReturn(Optional.empty());
        when(repositoryRepository.save(any(Repository.class))).thenAnswer(inv -> {
            Repository r = inv.getArgument(0);
            if (r.getId() == null) r.setId("repo-2");
            return r;
        });

        SkillGroup group2 = new SkillGroup();
        group2.setId("group-2");
        group2.setRepository(repo2);
        group2.setUser(testUser);
        group2.setName("repo2");
        when(skillGroupRepository.findByRepositoryId("repo-2")).thenReturn(Optional.empty());
        when(skillGroupRepository.save(any(SkillGroup.class))).thenAnswer(inv -> {
            SkillGroup sg = inv.getArgument(0);
            if (sg.getId() == null) sg.setId("group-2");
            return sg;
        });

        // A skill with the same name already exists in a DIFFERENT group
        SkillGroup otherGroup = new SkillGroup();
        otherGroup.setId("group-1");
        Skill conflictingSkill = new Skill();
        conflictingSkill.setId("existing-skill-id");
        conflictingSkill.setName("conflicting-skill");
        conflictingSkill.setFolderPath("skills/conflicting-skill");
        conflictingSkill.setSkillGroup(otherGroup);
        conflictingSkill.setUser(testUser);

        when(skillRepository.findByUserIdAndName("user-1", "conflicting-skill"))
                .thenReturn(Optional.of(conflictingSkill));

        assertThatThrownBy(() -> service.importRepository("https://github.com/owner/repo2", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Skill 名称 'conflicting-skill' 已被同一发布者的其他 Skill 组使用");
    }
}
