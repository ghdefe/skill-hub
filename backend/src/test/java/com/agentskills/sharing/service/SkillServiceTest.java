package com.agentskills.sharing.service;

import com.agentskills.sharing.dto.PageResponse;
import com.agentskills.sharing.dto.SkillDetailResponse;
import com.agentskills.sharing.dto.SkillGroupDetailResponse;
import com.agentskills.sharing.dto.SkillGroupListResponse;
import com.agentskills.sharing.dto.SkillListResponse;
import com.agentskills.sharing.dto.UpdateSkillGroupRequest;
import com.agentskills.sharing.entity.Repository;
import com.agentskills.sharing.entity.Skill;
import com.agentskills.sharing.entity.SkillGroup;
import com.agentskills.sharing.entity.SkillStatus;
import com.agentskills.sharing.entity.Tag;
import com.agentskills.sharing.entity.User;
import com.agentskills.sharing.repository.SkillGroupRepository;
import com.agentskills.sharing.repository.SkillRepository;
import com.agentskills.sharing.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private SkillGroupRepository skillGroupRepository;

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private SkillService skillService;

    private User testUser;
    private Repository testRepo;
    private SkillGroup testGroup;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("user-1");
        testUser.setUsername("testuser");

        testRepo = new Repository();
        testRepo.setId("repo-1");
        testRepo.setUrl("https://github.com/owner/repo");
        testRepo.setStarCount(100);
        testRepo.setForkCount(20);

        testGroup = new SkillGroup();
        testGroup.setId("group-1");
        testGroup.setName("test-group");
        testGroup.setDescription("A test group");
        testGroup.setRepository(testRepo);
        testGroup.setUser(testUser);
        testGroup.setDownloadCount(50);
        testGroup.setCreatedAt(LocalDateTime.now());
        testGroup.setUpdatedAt(LocalDateTime.now());
    }

    // --- listSkills tests ---

    @Test
    void listSkills_withNoFilters_shouldReturnPaginatedResults() {
        List<Skill> skills = List.of(createSkill("s1", "Alpha"), createSkill("s2", "Beta"));
        Page<Skill> page = new PageImpl<>(skills, Pageable.ofSize(20), 2);

        when(skillRepository.findByStatus(eq(SkillStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<SkillListResponse> result = skillService.listSkills(null, null, null, null, 1, 20);

        assertThat(result.items()).hasSize(2);
        assertThat(result.total()).isEqualTo(2);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.pageSize()).isEqualTo(20);
    }

    @Test
    void listSkills_withTagFilter_shouldUseTagIntersection() {
        List<Skill> skills = List.of(createSkill("s1", "Alpha"));
        Page<Skill> page = new PageImpl<>(skills, Pageable.ofSize(20), 1);

        when(skillRepository.findByStatusAndTags(
                eq(SkillStatus.ACTIVE), eq(List.of("nlp", "chat")), eq(2L), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<SkillListResponse> result = skillService.listSkills(null, "nlp,chat", null, null, 1, 20);

        assertThat(result.items()).hasSize(1);
        assertThat(result.total()).isEqualTo(1);
    }

    @Test
    void listSkills_withFtsQuery_shouldUseFtsSearch() {
        List<Skill> skills = List.of(createSkill("s1", "Agent Tool"), createSkill("s2", "Agent Helper"));

        when(skillRepository.searchByFts("agent")).thenReturn(skills);

        PageResponse<SkillListResponse> result = skillService.listSkills("agent", null, null, null, 1, 20);

        assertThat(result.items()).hasSize(2);
        assertThat(result.total()).isEqualTo(2);
    }

    @Test
    void listSkills_withFtsAndTags_shouldFilterFtsResultsByTags() {
        Skill s1 = createSkill("s1", "Agent Tool");
        Tag nlpTag = new Tag();
        nlpTag.setName("nlp");
        s1.setTags(Set.of(nlpTag));

        Skill s2 = createSkill("s2", "Agent Helper");
        // s2 has no tags

        when(skillRepository.searchByFts("agent")).thenReturn(List.of(s1, s2));

        PageResponse<SkillListResponse> result = skillService.listSkills("agent", "nlp", null, null, 1, 20);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).name()).isEqualTo("Agent Tool");
    }

    @Test
    void listSkills_withPaginationBeyondResults_shouldReturnEmptyItems() {
        List<Skill> skills = List.of(createSkill("s1", "Alpha"));
        when(skillRepository.searchByFts("test")).thenReturn(skills);

        PageResponse<SkillListResponse> result = skillService.listSkills("test", null, null, null, 5, 20);

        assertThat(result.items()).isEmpty();
        assertThat(result.total()).isEqualTo(1);
    }

    @Test
    void listSkills_withSortByName_shouldSortCorrectly() {
        Page<Skill> page = new PageImpl<>(List.of(createSkill("s1", "Alpha")), Pageable.ofSize(20), 1);
        when(skillRepository.findByStatus(eq(SkillStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<SkillListResponse> result = skillService.listSkills(null, null, "name", "asc", 1, 20);

        assertThat(result.items()).hasSize(1);
    }

    @Test
    void listSkills_withInvalidSort_shouldDefaultToCreatedAt() {
        Page<Skill> page = new PageImpl<>(List.of(), Pageable.ofSize(20), 0);
        when(skillRepository.findByStatus(eq(SkillStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<SkillListResponse> result = skillService.listSkills(null, null, "invalid", null, 1, 20);

        assertThat(result.page()).isEqualTo(1);
    }

    @Test
    void listSkills_withNegativePage_shouldClampToOne() {
        Page<Skill> page = new PageImpl<>(List.of(), Pageable.ofSize(20), 0);
        when(skillRepository.findByStatus(eq(SkillStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<SkillListResponse> result = skillService.listSkills(null, null, null, null, -1, 20);

        assertThat(result.page()).isEqualTo(1);
    }

    @Test
    void listSkills_withExcessivePageSize_shouldClampTo100() {
        Page<Skill> page = new PageImpl<>(List.of(), Pageable.ofSize(100), 0);
        when(skillRepository.findByStatus(eq(SkillStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<SkillListResponse> result = skillService.listSkills(null, null, null, null, 1, 500);

        assertThat(result.pageSize()).isEqualTo(100);
    }

    // --- getSkillDetail tests ---

    @Test
    void getSkillDetail_shouldReturnFullDetail() {
        Skill skill = createSkill("s1", "Test Skill");
        skill.setReadmeContent("# README");
        skill.setDescription("A test skill");

        when(skillRepository.findById("s1")).thenReturn(Optional.of(skill));

        SkillDetailResponse result = skillService.getSkillDetail("s1");

        assertThat(result.id()).isEqualTo("s1");
        assertThat(result.name()).isEqualTo("Test Skill");
        assertThat(result.readmeContent()).isEqualTo("# README");
        assertThat(result.author()).isEqualTo("testuser");
        assertThat(result.starCount()).isEqualTo(100);
        assertThat(result.forkCount()).isEqualTo(20);
        assertThat(result.repoUrl()).isEqualTo("https://github.com/owner/repo");
        assertThat(result.skillGroup()).isNotNull();
        assertThat(result.skillGroup().id()).isEqualTo("group-1");
    }

    @Test
    void getSkillDetail_withNonExistentId_shouldThrowNotFound() {
        when(skillRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> skillService.getSkillDetail("non-existent"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Skill 不存在");
    }

    // --- listSkillGroups tests ---

    @Test
    void listSkillGroups_shouldReturnAllGroups() {
        when(skillGroupRepository.findAll()).thenReturn(List.of(testGroup));

        List<SkillGroupListResponse> result = skillService.listSkillGroups();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("group-1");
        assertThat(result.get(0).name()).isEqualTo("test-group");
        assertThat(result.get(0).author()).isEqualTo("testuser");
    }

    // --- getSkillGroupDetail tests ---

    @Test
    void getSkillGroupDetail_shouldReturnDetailWithSkills() {
        Skill skill = createSkill("s1", "Skill A");
        skill.setDownloadCount(30);

        when(skillGroupRepository.findById("group-1")).thenReturn(Optional.of(testGroup));
        when(skillRepository.findBySkillGroupId("group-1")).thenReturn(List.of(skill));

        SkillGroupDetailResponse result = skillService.getSkillGroupDetail("group-1");

        assertThat(result.id()).isEqualTo("group-1");
        assertThat(result.name()).isEqualTo("test-group");
        assertThat(result.repoUrl()).isEqualTo("https://github.com/owner/repo");
        assertThat(result.skills()).hasSize(1);
        assertThat(result.totalSkillDownloads()).isEqualTo(30);
    }

    @Test
    void getSkillGroupDetail_withNonExistentId_shouldThrowNotFound() {
        when(skillGroupRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> skillService.getSkillGroupDetail("non-existent"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("SkillGroup 不存在");
    }

    // --- listTags tests ---

    @Test
    void listTags_shouldReturnAllTags() {
        Tag tag1 = new Tag();
        tag1.setId("t1");
        tag1.setName("nlp");
        Tag tag2 = new Tag();
        tag2.setId("t2");
        tag2.setName("chat");

        when(tagRepository.findAll()).thenReturn(List.of(tag1, tag2));

        List<com.agentskills.sharing.entity.Tag> result = skillService.listTags();

        assertThat(result).hasSize(2);
    }

    // --- recordSkillCopyEvent tests ---

    @Test
    void recordSkillCopyEvent_shouldIncrementDownloadCount() {
        Skill skill = createSkill("s1", "Test Skill");
        skill.setDownloadCount(10);

        when(skillRepository.findById("s1")).thenReturn(Optional.of(skill));
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));

        skillService.recordSkillCopyEvent("s1");

        assertThat(skill.getDownloadCount()).isEqualTo(11);
        verify(skillRepository).save(skill);
    }

    @Test
    void recordSkillCopyEvent_withNonExistentId_shouldThrowNotFound() {
        when(skillRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> skillService.recordSkillCopyEvent("non-existent"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Skill 不存在");
    }

    // --- recordSkillGroupCopyEvent tests ---

    @Test
    void recordSkillGroupCopyEvent_shouldIncrementDownloadCount() {
        testGroup.setDownloadCount(50);

        when(skillGroupRepository.findById("group-1")).thenReturn(Optional.of(testGroup));
        when(skillGroupRepository.save(any(SkillGroup.class))).thenAnswer(inv -> inv.getArgument(0));

        skillService.recordSkillGroupCopyEvent("group-1");

        assertThat(testGroup.getDownloadCount()).isEqualTo(51);
        verify(skillGroupRepository).save(testGroup);
    }

    @Test
    void recordSkillGroupCopyEvent_withNonExistentId_shouldThrowNotFound() {
        when(skillGroupRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> skillService.recordSkillGroupCopyEvent("non-existent"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("SkillGroup 不存在");
    }

    // --- updateSkillGroup tests ---

    @Test
    void updateSkillGroup_shouldUpdateNameAndDescription() {
        var request = new UpdateSkillGroupRequest("New Name", "New Description");

        when(skillGroupRepository.findById("group-1")).thenReturn(Optional.of(testGroup));
        when(skillGroupRepository.findByUserIdAndName("user-1", "New Name")).thenReturn(Optional.empty());
        when(skillGroupRepository.save(any(SkillGroup.class))).thenAnswer(inv -> inv.getArgument(0));
        when(skillRepository.findBySkillGroupId("group-1")).thenReturn(List.of());

        SkillGroupDetailResponse result = skillService.updateSkillGroup("group-1", request, "user-1");

        assertThat(result.name()).isEqualTo("New Name");
        assertThat(result.description()).isEqualTo("New Description");
        verify(skillGroupRepository).save(testGroup);
    }

    @Test
    void updateSkillGroup_withOnlyName_shouldUpdateNameOnly() {
        var request = new UpdateSkillGroupRequest("Updated Name", null);

        when(skillGroupRepository.findById("group-1")).thenReturn(Optional.of(testGroup));
        when(skillGroupRepository.findByUserIdAndName("user-1", "Updated Name")).thenReturn(Optional.empty());
        when(skillGroupRepository.save(any(SkillGroup.class))).thenAnswer(inv -> inv.getArgument(0));
        when(skillRepository.findBySkillGroupId("group-1")).thenReturn(List.of());

        SkillGroupDetailResponse result = skillService.updateSkillGroup("group-1", request, "user-1");

        assertThat(result.name()).isEqualTo("Updated Name");
        assertThat(result.description()).isEqualTo("A test group"); // unchanged
    }

    @Test
    void updateSkillGroup_withOnlyDescription_shouldUpdateDescriptionOnly() {
        var request = new UpdateSkillGroupRequest(null, "Updated Description");

        when(skillGroupRepository.findById("group-1")).thenReturn(Optional.of(testGroup));
        when(skillGroupRepository.save(any(SkillGroup.class))).thenAnswer(inv -> inv.getArgument(0));
        when(skillRepository.findBySkillGroupId("group-1")).thenReturn(List.of());

        SkillGroupDetailResponse result = skillService.updateSkillGroup("group-1", request, "user-1");

        assertThat(result.name()).isEqualTo("test-group"); // unchanged
        assertThat(result.description()).isEqualTo("Updated Description");
    }

    @Test
    void updateSkillGroup_withSameName_shouldNotCheckUniqueness() {
        var request = new UpdateSkillGroupRequest("test-group", "New Desc");

        when(skillGroupRepository.findById("group-1")).thenReturn(Optional.of(testGroup));
        when(skillGroupRepository.save(any(SkillGroup.class))).thenAnswer(inv -> inv.getArgument(0));
        when(skillRepository.findBySkillGroupId("group-1")).thenReturn(List.of());

        SkillGroupDetailResponse result = skillService.updateSkillGroup("group-1", request, "user-1");

        assertThat(result.name()).isEqualTo("test-group");
        assertThat(result.description()).isEqualTo("New Desc");
        // findByUserIdAndName should NOT be called since name didn't change
    }

    @Test
    void updateSkillGroup_withNonExistentId_shouldThrowNotFound() {
        var request = new UpdateSkillGroupRequest("Name", null);
        when(skillGroupRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> skillService.updateSkillGroup("non-existent", request, "user-1"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("SkillGroup 不存在");
    }

    @Test
    void updateSkillGroup_withNonOwner_shouldThrowSecurityException() {
        var request = new UpdateSkillGroupRequest("Name", null);
        when(skillGroupRepository.findById("group-1")).thenReturn(Optional.of(testGroup));

        assertThatThrownBy(() -> skillService.updateSkillGroup("group-1", request, "other-user"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("无权编辑此 SkillGroup");
    }

    @Test
    void updateSkillGroup_withDuplicateName_shouldThrowConflict() {
        var request = new UpdateSkillGroupRequest("Existing Name", null);
        SkillGroup existingGroup = new SkillGroup();
        existingGroup.setId("group-2");
        existingGroup.setName("Existing Name");

        when(skillGroupRepository.findById("group-1")).thenReturn(Optional.of(testGroup));
        when(skillGroupRepository.findByUserIdAndName("user-1", "Existing Name"))
                .thenReturn(Optional.of(existingGroup));

        assertThatThrownBy(() -> skillService.updateSkillGroup("group-1", request, "user-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("同一发布者下已存在名为");
    }

    // --- Helper methods ---

    private Skill createSkill(String id, String name) {
        Skill skill = new Skill();
        skill.setId(id);
        skill.setName(name);
        skill.setDescription("Description of " + name);
        skill.setFolderPath("skills/" + name.toLowerCase().replace(" ", "-"));
        skill.setDownloadCount(10);
        skill.setStatus(SkillStatus.ACTIVE);
        skill.setCreatedAt(LocalDateTime.now());
        skill.setUpdatedAt(LocalDateTime.now());
        skill.setSkillGroup(testGroup);
        skill.setUser(testUser);
        skill.setTags(new HashSet<>());
        return skill;
    }
}
