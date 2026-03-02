package com.agentskills.sharing.controller;

import com.agentskills.sharing.dto.PageResponse;
import com.agentskills.sharing.dto.SkillDetailResponse;
import com.agentskills.sharing.dto.SkillListResponse;
import com.agentskills.sharing.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API controller for Skill browsing and searching.
 *
 * <ul>
 *   <li>GET /api/skills - List/search skills with filtering, sorting, pagination</li>
 *   <li>GET /api/skills/{id} - Get skill detail</li>
 *   <li>POST /api/skills/{id}/copy-event - Record skill copy event (download count +1)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    /**
     * List or search skills with optional FTS5 search, tag filtering, sorting, and pagination.
     *
     * @param q        search query (FTS5 full-text search)
     * @param tags     comma-separated tag names (intersection filtering)
     * @param sort     sort field: downloads|createdAt|name|starCount
     * @param order    sort direction: asc|desc
     * @param page     page number (1-based, default 1)
     * @param pageSize items per page (default 20, max 100)
     */
    @GetMapping
    public ResponseEntity<PageResponse<SkillListResponse>> listSkills(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false, defaultValue = "createdAt") String sort,
            @RequestParam(required = false, defaultValue = "desc") String order,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int pageSize) {

        PageResponse<SkillListResponse> response = skillService.listSkills(q, tags, sort, order, page, pageSize);
        return ResponseEntity.ok(response);
    }

    /**
     * Get full skill detail by ID, including README, SkillGroup info, and star/fork counts.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SkillDetailResponse> getSkillDetail(@PathVariable String id) {
        SkillDetailResponse response = skillService.getSkillDetail(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Record a Skill copy event (increment download count by 1).
     * No authentication required — consumers can copy without logging in.
     */
    @PostMapping("/{id}/copy-event")
    public ResponseEntity<Void> recordCopyEvent(@PathVariable String id) {
        skillService.recordSkillCopyEvent(id);
        return ResponseEntity.ok().build();
    }
}
