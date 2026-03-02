package com.agentskills.sharing.controller;

import com.agentskills.sharing.entity.Tag;
import com.agentskills.sharing.service.SkillService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagControllerTest {

    @Mock
    private SkillService skillService;

    @InjectMocks
    private TagController tagController;

    @Test
    void listTags_shouldReturn200WithTags() {
        Tag tag1 = new Tag();
        tag1.setId("t1");
        tag1.setName("nlp");
        tag1.setCreatedAt(LocalDateTime.now());

        Tag tag2 = new Tag();
        tag2.setId("t2");
        tag2.setName("chat");
        tag2.setCreatedAt(LocalDateTime.now());

        when(skillService.listTags()).thenReturn(List.of(tag1, tag2));

        var result = tagController.listTags();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(2);
    }

    @Test
    void listTags_withNoTags_shouldReturnEmptyList() {
        when(skillService.listTags()).thenReturn(List.of());

        var result = tagController.listTags();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEmpty();
    }
}
