package com.agentskills.sharing.controller;

import com.agentskills.sharing.entity.Tag;
import com.agentskills.sharing.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API controller for Tag listing.
 *
 * <ul>
 *   <li>GET /api/tags - List all tags</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final SkillService skillService;

    /**
     * List all available tags.
     */
    @GetMapping
    public ResponseEntity<List<Tag>> listTags() {
        List<Tag> tags = skillService.listTags();
        return ResponseEntity.ok(tags);
    }
}
