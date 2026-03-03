package com.agentskills.sharing.service;

import com.agentskills.sharing.dto.PageResponse;
import com.agentskills.sharing.dto.SkillDetailResponse;
import com.agentskills.sharing.dto.SkillGroupDetailResponse;
import com.agentskills.sharing.dto.SkillGroupListResponse;
import com.agentskills.sharing.dto.SkillListResponse;
import com.agentskills.sharing.dto.UpdateSkillGroupRequest;
import com.agentskills.sharing.entity.Skill;
import com.agentskills.sharing.entity.SkillGroup;
import com.agentskills.sharing.entity.SkillStatus;
import com.agentskills.sharing.entity.Tag;
import com.agentskills.sharing.repository.SkillGroupRepository;
import com.agentskills.sharing.repository.SkillRepository;
import com.agentskills.sharing.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Service handling skill search, filtering, sorting, and pagination logic.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SkillService {

    private final SkillRepository skillRepository;
    private final SkillGroupRepository skillGroupRepository;
    private final TagRepository tagRepository;

    private static final Set<String> VALID_SORT_FIELDS = Set.of(
            "downloads", "createdAt", "name", "starCount"
    );

    /**
     * Search and list skills with optional FTS5 search, tag filtering, sorting, and pagination.
     */
    public PageResponse<SkillListResponse> listSkills(
            String q, String tags, String sort, String order, int page, int pageSize) {

        // Validate and normalize parameters
        page = Math.max(1, page);
        pageSize = Math.max(1, Math.min(pageSize, 100));
        sort = (sort != null && VALID_SORT_FIELDS.contains(sort)) ? sort : "createdAt";
        order = "asc".equalsIgnoreCase(order) ? "asc" : "desc";

        List<String> tagList = parseTags(tags);

        // If FTS5 search query is provided, use native search
        if (q != null && !q.isBlank()) {
            return searchByFts(q, tagList, sort, order, page, pageSize);
        }

        // Standard listing with optional tag filtering
        return listWithFilters(tagList, sort, order, page, pageSize);
    }

    /**
     * Get skill detail by ID.
     */
    public SkillDetailResponse getSkillDetail(String id) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Skill 不存在: " + id));

        // Eagerly load associations for DTO mapping
        skill.getSkillGroup().getRepository().getStarCount();
        skill.getUser().getUsername();
        skill.getTags().size();

        return SkillDetailResponse.from(skill);
    }

    /**
     * List all skill groups.
     */
    public List<SkillGroupListResponse> listSkillGroups(String query) {
        List<SkillGroup> groups = (query != null && !query.isBlank())
                ? skillGroupRepository.findByNameContainingIgnoreCase(query.trim())
                : skillGroupRepository.findAll();
        return groups.stream()
                .map(group -> {
                    int skillCount = skillRepository.findBySkillGroupIdAndStatus(
                            group.getId(), SkillStatus.ACTIVE).size();
                    return SkillGroupListResponse.from(group, skillCount);
                })
                .toList();
    }

    /**
     * Get skill group detail by ID, including skill list and total download count.
     */
    public SkillGroupDetailResponse getSkillGroupDetail(String id) {
        SkillGroup group = skillGroupRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("SkillGroup 不存在: " + id));

        // Eagerly load associations
        group.getRepository().getUrl();
        group.getUser().getUsername();

        List<Skill> skills = skillRepository.findBySkillGroupId(group.getId());
        return SkillGroupDetailResponse.from(group, skills);
    }

    /**
     * List all tags.
     */
    public List<Tag> listTags() {
        return tagRepository.findAll();
    }

    /**
     * Record a copy event for a Skill (increment download count by 1).
     *
     * @param id the Skill ID
     * @throws NoSuchElementException if the Skill does not exist
     */
    @Transactional
    public void recordSkillCopyEvent(String id) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Skill 不存在: " + id));
        skill.setDownloadCount(skill.getDownloadCount() + 1);
        skillRepository.save(skill);
    }

    /**
     * Record a copy event for a SkillGroup (increment download count for the group
     * and each active skill within it).
     *
     * @param id the SkillGroup ID
     * @throws NoSuchElementException if the SkillGroup does not exist
     */
    @Transactional
    public void recordSkillGroupCopyEvent(String id) {
        SkillGroup group = skillGroupRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("SkillGroup 不存在: " + id));
        group.setDownloadCount(group.getDownloadCount() + 1);
        skillGroupRepository.save(group);

        // Also increment each active skill's download count
        List<Skill> activeSkills = skillRepository.findBySkillGroupIdAndStatus(
                id, SkillStatus.ACTIVE);
        for (Skill skill : activeSkills) {
            skill.setDownloadCount(skill.getDownloadCount() + 1);
        }
        skillRepository.saveAll(activeSkills);
    }

    /**
     * Update a SkillGroup's name and/or description.
     * Only the owner (publisher) can edit the SkillGroup.
     * SkillGroup name must be unique within the same publisher.
     *
     * @param id      the SkillGroup ID
     * @param request the update request with optional name and description
     * @param userId  the authenticated user's ID
     * @return the updated SkillGroup detail response
     * @throws NoSuchElementException if the SkillGroup does not exist
     * @throws SecurityException      if the user is not the owner
     * @throws IllegalStateException  if the name conflicts with another SkillGroup of the same publisher
     */
    @Transactional
    public SkillGroupDetailResponse updateSkillGroup(String id, UpdateSkillGroupRequest request, String userId) {
        SkillGroup group = skillGroupRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("SkillGroup 不存在: " + id));

        // Verify ownership
        if (!group.getUser().getId().equals(userId)) {
            throw new SecurityException("无权编辑此 SkillGroup");
        }

        // Update name if provided
        if (request.name() != null) {
            String newName = request.name().trim();
            // Check uniqueness only if name actually changed
            if (!newName.equals(group.getName())) {
                skillGroupRepository.findByUserIdAndName(userId, newName)
                        .ifPresent(existing -> {
                            throw new IllegalStateException("同一发布者下已存在名为 \"" + newName + "\" 的 SkillGroup");
                        });
                group.setName(newName);
            }
        }

        // Update description if provided
        if (request.description() != null) {
            group.setDescription(request.description());
        }

        skillGroupRepository.save(group);

        List<Skill> skills = skillRepository.findBySkillGroupId(group.getId());
        return SkillGroupDetailResponse.from(group, skills);
    }



    // --- Private helpers ---

    private List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private PageResponse<SkillListResponse> searchByFts(
            String query, List<String> tagList, String sort, String order, int page, int pageSize) {

        // FTS search first, fall back to fuzzy ILIKE if no results
        List<Skill> ftsResults = skillRepository.searchByFts(query);
        if (ftsResults.isEmpty()) {
            ftsResults = skillRepository.searchByFuzzy(query);
        }

        // Apply tag filtering if needed
        if (!tagList.isEmpty()) {
            Set<String> requiredTags = new HashSet<>(tagList);
            ftsResults = ftsResults.stream()
                    .filter(skill -> {
                        Set<String> skillTags = new HashSet<>();
                        skill.getTags().forEach(t -> skillTags.add(t.getName()));
                        return skillTags.containsAll(requiredTags);
                    })
                    .toList();
        }

        // Apply sorting (FTS5 default is by relevance rank; override if explicit sort requested)
        if (!"createdAt".equals(sort) || "asc".equals(order)) {
            ftsResults = sortSkills(ftsResults, sort, order);
        }

        // Apply pagination
        long total = ftsResults.size();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, ftsResults.size());

        List<SkillListResponse> items;
        if (fromIndex >= ftsResults.size()) {
            items = List.of();
        } else {
            items = ftsResults.subList(fromIndex, toIndex).stream()
                    .map(SkillListResponse::from)
                    .toList();
        }

        return new PageResponse<>(items, total, page, pageSize);
    }

    private PageResponse<SkillListResponse> listWithFilters(
            List<String> tagList, String sort, String order, int page, int pageSize) {

        Sort springSort = buildSort(sort, order);
        Pageable pageable = PageRequest.of(page - 1, pageSize, springSort);

        Page<Skill> skillPage;
        if (!tagList.isEmpty()) {
            skillPage = skillRepository.findByStatusAndTags(
                    SkillStatus.ACTIVE, tagList, tagList.size(), pageable);
        } else {
            skillPage = skillRepository.findByStatus(SkillStatus.ACTIVE, pageable);
        }

        List<SkillListResponse> items = skillPage.getContent().stream()
                .map(SkillListResponse::from)
                .toList();

        return new PageResponse<>(items, skillPage.getTotalElements(), page, pageSize);
    }

    private Sort buildSort(String sort, String order) {
        String field = mapSortField(sort);
        Sort.Direction direction = "asc".equals(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, field);
    }

    private String mapSortField(String sort) {
        return switch (sort) {
            case "downloads" -> "downloadCount";
            case "name" -> "name";
            case "starCount" -> "skillGroup.repository.starCount";
            default -> "createdAt";
        };
    }

    private List<Skill> sortSkills(List<Skill> skills, String sort, String order) {
        Comparator<Skill> comparator = switch (sort) {
            case "downloads" -> Comparator.comparing(Skill::getDownloadCount);
            case "name" -> Comparator.comparing(Skill::getName, String.CASE_INSENSITIVE_ORDER);
            case "starCount" -> Comparator.comparing(s ->
                    s.getSkillGroup() != null && s.getSkillGroup().getRepository() != null
                            ? s.getSkillGroup().getRepository().getStarCount() : 0);
            default -> Comparator.comparing(Skill::getCreatedAt);
        };

        if ("desc".equals(order)) {
            comparator = comparator.reversed();
        }

        return skills.stream().sorted(comparator).toList();
    }
}
