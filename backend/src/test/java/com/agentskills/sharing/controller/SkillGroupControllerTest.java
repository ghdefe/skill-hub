package com.agentskills.sharing.controller;

import com.agentskills.sharing.dto.SkillGroupDetailResponse;
import com.agentskills.sharing.dto.SkillGroupListResponse;
import com.agentskills.sharing.dto.UpdateSkillGroupRequest;
import com.agentskills.sharing.service.SkillService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillGroupControllerTest {

    @Mock
    private SkillService skillService;

    @InjectMocks
    private SkillGroupController skillGroupController;

    @Test
    void listSkillGroups_shouldReturn200WithGroups() {
        var group = new SkillGroupListResponse("g1", "group", "desc", "user", 50, LocalDateTime.now());
        when(skillService.listSkillGroups()).thenReturn(List.of(group));

        var result = skillGroupController.listSkillGroups();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
        assertThat(result.getBody().get(0).id()).isEqualTo("g1");
    }

    @Test
    void listSkillGroups_withNoGroups_shouldReturnEmptyList() {
        when(skillService.listSkillGroups()).thenReturn(List.of());

        var result = skillGroupController.listSkillGroups();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEmpty();
    }

    @Test
    void getSkillGroupDetail_shouldReturn200WithDetail() {
        var skill = new SkillGroupDetailResponse.SkillSummary("s1", "Skill A", "desc", 10, "ACTIVE", "skills/a");
        var detail = new SkillGroupDetailResponse(
                "g1", "group", "desc", "user", "https://github.com/o/r",
                50, 10, List.of(skill), LocalDateTime.now(), LocalDateTime.now());

        when(skillService.getSkillGroupDetail("g1")).thenReturn(detail);

        var result = skillGroupController.getSkillGroupDetail("g1");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().skills()).hasSize(1);
        assertThat(result.getBody().totalSkillDownloads()).isEqualTo(10);
    }

    @Test
    void getSkillGroupDetail_withNonExistentId_shouldPropagateException() {
        when(skillService.getSkillGroupDetail("bad-id"))
                .thenThrow(new NoSuchElementException("SkillGroup 不存在: bad-id"));

        assertThatThrownBy(() -> skillGroupController.getSkillGroupDetail("bad-id"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void recordCopyEvent_shouldReturn200OnSuccess() {
        doNothing().when(skillService).recordSkillGroupCopyEvent("g1");

        var result = skillGroupController.recordCopyEvent("g1");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(skillService).recordSkillGroupCopyEvent("g1");
    }

    @Test
    void recordCopyEvent_withNonExistentId_shouldPropagateException() {
        doThrow(new NoSuchElementException("SkillGroup 不存在: bad-id"))
                .when(skillService).recordSkillGroupCopyEvent("bad-id");

        assertThatThrownBy(() -> skillGroupController.recordCopyEvent("bad-id"))
                .isInstanceOf(NoSuchElementException.class);
    }

    // --- updateSkillGroup (PATCH) tests ---

    @Test
    void updateSkillGroup_shouldReturn200WithUpdatedDetail() {
        var request = new UpdateSkillGroupRequest("New Name", "New Description");
        var detail = new SkillGroupDetailResponse(
                "g1", "New Name", "New Description", "user", "https://github.com/o/r",
                50, 10, List.of(), LocalDateTime.now(), LocalDateTime.now());

        Authentication auth = mockAuthentication("user-1");
        when(skillService.updateSkillGroup("g1", request, "user-1")).thenReturn(detail);

        var result = skillGroupController.updateSkillGroup("g1", request, auth);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().name()).isEqualTo("New Name");
        assertThat(result.getBody().description()).isEqualTo("New Description");
    }

    @Test
    void updateSkillGroup_withNonExistentId_shouldPropagateNotFoundException() {
        var request = new UpdateSkillGroupRequest("Name", null);
        Authentication auth = mockAuthentication("user-1");

        when(skillService.updateSkillGroup("bad-id", request, "user-1"))
                .thenThrow(new NoSuchElementException("SkillGroup 不存在: bad-id"));

        assertThatThrownBy(() -> skillGroupController.updateSkillGroup("bad-id", request, auth))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void updateSkillGroup_withNonOwner_shouldPropagateSecurityException() {
        var request = new UpdateSkillGroupRequest("Name", null);
        Authentication auth = mockAuthentication("other-user");

        when(skillService.updateSkillGroup("g1", request, "other-user"))
                .thenThrow(new SecurityException("无权编辑此 SkillGroup"));

        assertThatThrownBy(() -> skillGroupController.updateSkillGroup("g1", request, auth))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void updateSkillGroup_withDuplicateName_shouldPropagateIllegalStateException() {
        var request = new UpdateSkillGroupRequest("Existing Name", null);
        Authentication auth = mockAuthentication("user-1");

        when(skillService.updateSkillGroup("g1", request, "user-1"))
                .thenThrow(new IllegalStateException("同一发布者下已存在名为 \"Existing Name\" 的 SkillGroup"));

        assertThatThrownBy(() -> skillGroupController.updateSkillGroup("g1", request, auth))
                .isInstanceOf(IllegalStateException.class);
    }

    private Authentication mockAuthentication(String userId) {
        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userId);
        return auth;
    }
}
