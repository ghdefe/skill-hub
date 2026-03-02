package com.agentskills.sharing.controller;

import com.agentskills.sharing.dto.PageResponse;
import com.agentskills.sharing.dto.SkillDetailResponse;
import com.agentskills.sharing.dto.SkillListResponse;
import com.agentskills.sharing.service.SkillService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

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
class SkillControllerTest {

    @Mock
    private SkillService skillService;

    @InjectMocks
    private SkillController skillController;

    @Test
    void listSkills_shouldDelegateToServiceAndReturn200() {
        var item = new SkillListResponse("s1", "Test", "desc", "user", 10, 5, 2, List.of("nlp"), null, LocalDateTime.now());
        var pageResponse = new PageResponse<>(List.of(item), 1L, 1, 20);

        when(skillService.listSkills("agent", "nlp", "downloads", "desc", 1, 20))
                .thenReturn(pageResponse);

        var result = skillController.listSkills("agent", "nlp", "downloads", "desc", 1, 20);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().items()).hasSize(1);
        assertThat(result.getBody().total()).isEqualTo(1);
    }

    @Test
    void listSkills_withDefaults_shouldPassDefaultValues() {
        var pageResponse = new PageResponse<SkillListResponse>(List.of(), 0L, 1, 20);

        when(skillService.listSkills(null, null, "createdAt", "desc", 1, 20))
                .thenReturn(pageResponse);

        var result = skillController.listSkills(null, null, "createdAt", "desc", 1, 20);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().items()).isEmpty();
    }

    @Test
    void getSkillDetail_shouldReturn200WithDetail() {
        var detail = new SkillDetailResponse(
                "s1", "Test", "desc", "# README", "user", 10, 5, 2,
                List.of("nlp"), "https://github.com/o/r", "skills/test",
                new SkillDetailResponse.SkillGroupInfo("g1", "group", "desc"),
                null, LocalDateTime.now(), LocalDateTime.now());

        when(skillService.getSkillDetail("s1")).thenReturn(detail);

        var result = skillController.getSkillDetail("s1");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().id()).isEqualTo("s1");
        assertThat(result.getBody().readmeContent()).isEqualTo("# README");
    }

    @Test
    void getSkillDetail_withNonExistentId_shouldPropagateException() {
        when(skillService.getSkillDetail("bad-id"))
                .thenThrow(new NoSuchElementException("Skill 不存在: bad-id"));

        assertThatThrownBy(() -> skillController.getSkillDetail("bad-id"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void recordCopyEvent_shouldReturn200OnSuccess() {
        doNothing().when(skillService).recordSkillCopyEvent("s1");

        var result = skillController.recordCopyEvent("s1");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(skillService).recordSkillCopyEvent("s1");
    }

    @Test
    void recordCopyEvent_withNonExistentId_shouldPropagateException() {
        doThrow(new NoSuchElementException("Skill 不存在: bad-id"))
                .when(skillService).recordSkillCopyEvent("bad-id");

        assertThatThrownBy(() -> skillController.recordCopyEvent("bad-id"))
                .isInstanceOf(NoSuchElementException.class);
    }
}
