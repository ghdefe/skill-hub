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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service responsible for scanning GitHub repositories, parsing skills/ directory,
 * and creating Repository, SkillGroup, Skill, and Tag records.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RepositoryScannerService {

    private static final Pattern GITHUB_URL_PATTERN =
            Pattern.compile("^https://github\\.com/([^/]+)/([^/]+?)(?:\\.git)?/?$");

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private final GitHubApiClient gitHubApiClient;
    private final UserRepository userRepository;
    private final RepositoryRepository repositoryRepository;
    private final SkillGroupRepository skillGroupRepository;
    private final SkillRepository skillRepository;
    private final TagRepository tagRepository;
    private final EncryptionUtil encryptionUtil;

    /**
     * Import a GitHub repository: validate URL, scan skills/ directory,
     * parse each subfolder into Skill records, and create associated entities.
     *
     * @param repoUrl the GitHub repository URL
     * @param userId  the authenticated user's ID
     * @return the created or updated Repository entity
     */
    @Transactional
    public Repository importRepository(String repoUrl, String userId) {
        // 1. Validate URL format and extract owner/repo
        Matcher matcher = GITHUB_URL_PATTERN.matcher(repoUrl.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("仓库 URL 格式无效");
        }
        String owner = matcher.group(1);
        String repo = matcher.group(2);

        // 2. Get user and decrypt access token
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("用户不存在"));
        String token = encryptionUtil.decrypt(user.getAccessToken());

        // 3. Read skills/ directory from GitHub
        List<GitHubContentItem> skillsDir = fetchSkillsDirectory(owner, repo, token);

        // 4. Filter for directories only
        List<GitHubContentItem> subDirs = skillsDir.stream()
                .filter(item -> "dir".equals(item.type()))
                .toList();

        if (subDirs.isEmpty()) {
            throw new IllegalArgumentException("skills/ 目录下未找到任何 Skill，请确认目录结构");
        }

        // 5. Create or find Repository entity
        Repository repoEntity = repositoryRepository
                .findByUserIdAndGithubOwnerAndGithubRepo(userId, owner, repo)
                .orElseGet(() -> {
                    Repository newRepo = new Repository();
                    newRepo.setUser(user);
                    newRepo.setGithubOwner(owner);
                    newRepo.setGithubRepo(repo);
                    newRepo.setUrl(repoUrl.trim());
                    return newRepo;
                });

        // 6. Get repo info (star/fork counts) and update
        try {
            GitHubRepoInfo repoInfo = gitHubApiClient.getRepoInfo(owner, repo, token);
            repoEntity.setStarCount(repoInfo.stargazersCount());
            repoEntity.setForkCount(repoInfo.forksCount());
        } catch (Exception e) {
            log.warn("Failed to fetch repo info for {}/{}: {}", owner, repo, e.getMessage());
        }
        repoEntity.setLastSyncedAt(LocalDateTime.now());
        Repository savedRepo = repositoryRepository.save(repoEntity);

        // 7. Create or find SkillGroup
        SkillGroup skillGroup = skillGroupRepository.findByRepositoryId(savedRepo.getId())
                .orElseGet(() -> {
                    SkillGroup newGroup = new SkillGroup();
                    newGroup.setRepository(savedRepo);
                    newGroup.setUser(user);
                    newGroup.setName(repo);
                    return newGroup;
                });
        skillGroup = skillGroupRepository.save(skillGroup);

        // 8. Parse each subdirectory and create Skill records, tracking scanned folder paths
        Set<String> scannedFolderPaths = new HashSet<>();
        for (GitHubContentItem dir : subDirs) {
            String folderPath = "skills/" + dir.name();
            scannedFolderPaths.add(folderPath);
            parseAndCreateSkill(dir, owner, repo, token, skillGroup, user);
        }

        // 9. Diff detection: mark skills that are no longer in the scan as REMOVED
        markRemovedSkills(skillGroup, scannedFolderPaths);

        return savedRepo;
    }

    /**
     * Mark skills in the SkillGroup that are no longer present in the new scan as REMOVED.
     * Skills whose folderPath is not in the scannedFolderPaths set will be marked REMOVED.
     */
    private void markRemovedSkills(SkillGroup skillGroup, Set<String> scannedFolderPaths) {
        List<Skill> existingSkills = skillRepository.findBySkillGroupId(skillGroup.getId());
        for (Skill existing : existingSkills) {
            if (!scannedFolderPaths.contains(existing.getFolderPath())) {
                if (existing.getStatus() != SkillStatus.REMOVED) {
                    existing.setStatus(SkillStatus.REMOVED);
                    skillRepository.save(existing);
                    log.info("Marked skill as REMOVED: {} (folder: {})", existing.getName(), existing.getFolderPath());
                }
            }
        }
    }

    /**
     * Fetch the skills/ directory contents from GitHub, handling errors.
     */
    private List<GitHubContentItem> fetchSkillsDirectory(String owner, String repo, String token) {
        try {
            return gitHubApiClient.getRepoContents(owner, repo, "skills", token);
        } catch (GitHubApiException e) {
            if (e.getStatusCode() == 404) {
                throw new IllegalArgumentException("未找到 skills/ 目录，请确认仓库结构");
            }
            throw new IllegalArgumentException("仓库不可访问，请检查 URL 和权限");
        } catch (Exception e) {
            log.error("Failed to access repository {}/{}: {}", owner, repo, e.getMessage());
            throw new IllegalArgumentException("仓库不可访问，请检查 URL 和权限");
        }
    }

    /**
     * Parse a single skill subdirectory: read manifest and README, then create/update Skill entity.
     */
    private void parseAndCreateSkill(GitHubContentItem dir, String owner, String repo,
                                     String token, SkillGroup skillGroup, User user) {
        String folderName = dir.name();
        String folderPath = "skills/" + folderName;

        // Try to read manifest (json first, then yaml)
        ManifestData manifest = readManifest(owner, repo, folderPath, token);

        // Try to read README.md
        String readmeContent = readReadme(owner, repo, folderPath, token);

        // Determine skill name and description
        String skillName = (manifest != null && manifest.name != null && !manifest.name.isBlank())
                ? manifest.name : folderName;
        String description = (manifest != null && manifest.description != null && !manifest.description.isBlank())
                ? manifest.description : extractFirstLine(readmeContent);

        // Create or update Skill
        Skill skill = skillRepository.findByUserIdAndName(user.getId(), skillName)
                .orElseGet(() -> {
                    Skill newSkill = new Skill();
                    newSkill.setUser(user);
                    newSkill.setName(skillName);
                    return newSkill;
                });

        // Skill name uniqueness validation: reject if same name exists in a different SkillGroup
        if (skill.getId() != null && skill.getSkillGroup() != null
                && !skill.getSkillGroup().getId().equals(skillGroup.getId())) {
            throw new IllegalArgumentException(
                    "Skill 名称 '" + skillName + "' 已被同一发布者的其他 Skill 组使用");
        }

        skill.setSkillGroup(skillGroup);
        skill.setDescription(description);
        skill.setReadmeContent(readmeContent);
        skill.setFolderPath(folderPath);
        skill.setStatus(SkillStatus.ACTIVE);

        // Handle tags
        if (manifest != null && manifest.tags != null && !manifest.tags.isEmpty()) {
            Set<Tag> tagEntities = new HashSet<>();
            for (String tagName : manifest.tags) {
                String normalizedTag = tagName.trim().toLowerCase();
                if (!normalizedTag.isEmpty()) {
                    Tag tag = tagRepository.findByName(normalizedTag)
                            .orElseGet(() -> {
                                Tag newTag = new Tag();
                                newTag.setName(normalizedTag);
                                return tagRepository.save(newTag);
                            });
                    tagEntities.add(tag);
                }
            }
            skill.setTags(tagEntities);
        }

        skillRepository.save(skill);
        log.info("Parsed skill: {} (folder: {})", skillName, folderPath);
    }

    /**
     * Try to read manifest.json or manifest.yaml from a skill folder.
     * Returns null if neither exists.
     */
    private ManifestData readManifest(String owner, String repo, String folderPath, String token) {
        // Try manifest.json first
        ManifestData manifest = tryReadManifest(owner, repo, folderPath + "/manifest.json", token, JSON_MAPPER);
        if (manifest != null) {
            return manifest;
        }
        // Fall back to manifest.yaml
        return tryReadManifest(owner, repo, folderPath + "/manifest.yaml", token, YAML_MAPPER);
    }

    private ManifestData tryReadManifest(String owner, String repo, String path, String token, ObjectMapper mapper) {
        try {
            String content = gitHubApiClient.getFileContent(owner, repo, path, token);
            if (content != null && !content.isBlank()) {
                Map<String, Object> data = mapper.readValue(content, new TypeReference<>() {});
                ManifestData manifest = new ManifestData();
                manifest.name = data.get("name") instanceof String s ? s : null;
                manifest.description = data.get("description") instanceof String s ? s : null;
                Object tagsObj = data.get("tags");
                if (tagsObj instanceof List<?> tagList) {
                    manifest.tags = tagList.stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .toList();
                }
                return manifest;
            }
        } catch (GitHubApiException e) {
            if (e.getStatusCode() != 404) {
                log.warn("Error reading manifest at {}: {}", path, e.getMessage());
            }
        } catch (Exception e) {
            log.warn("Failed to parse manifest at {}: {}", path, e.getMessage());
        }
        return null;
    }

    /**
     * Try to read README.md from a skill folder. Returns null if not found.
     */
    private String readReadme(String owner, String repo, String folderPath, String token) {
        try {
            return gitHubApiClient.getFileContent(owner, repo, folderPath + "/README.md", token);
        } catch (GitHubApiException e) {
            if (e.getStatusCode() != 404) {
                log.warn("Error reading README at {}/README.md: {}", folderPath, e.getMessage());
            }
        } catch (Exception e) {
            log.warn("Failed to read README at {}/README.md: {}", folderPath, e.getMessage());
        }
        return null;
    }

    /**
     * Extract the first non-empty line from text content (used as fallback description).
     */
    private String extractFirstLine(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        return content.lines()
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(line -> line.startsWith("#") ? line.replaceFirst("^#+\\s*", "") : line)
                .findFirst()
                .orElse(null);
    }

    /**
     * Internal DTO for parsed manifest data.
     */
    private static class ManifestData {
        String name;
        String description;
        List<String> tags;
    }
}
