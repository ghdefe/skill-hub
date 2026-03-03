package com.agentskills.sharing.controller;

import com.agentskills.sharing.dto.SkillGroupDetailResponse;
import com.agentskills.sharing.dto.SkillGroupListResponse;
import com.agentskills.sharing.dto.UpdateSkillGroupRequest;
import com.agentskills.sharing.service.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API controller for SkillGroup browsing and management.
 *
 * <ul>
 *   <li>GET /api/skill-groups - List all skill groups</li>
 *   <li>GET /api/skill-groups/{id} - Get skill group detail</li>
 *   <li>PATCH /api/skill-groups/{id} - Edit skill group name and description (auth required)</li>
 *   <li>POST /api/skill-groups/{id}/copy-event - Record skill group copy event (download count +1)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/skill-groups")
@RequiredArgsConstructor
public class SkillGroupController {

    private final SkillService skillService;

    /**
     * List all skill groups with basic info. Supports optional name search.
     */
    @GetMapping
    public ResponseEntity<List<SkillGroupListResponse>> listSkillGroups(
            @RequestParam(required = false) String q) {
        List<SkillGroupListResponse> response = skillService.listSkillGroups(q);
        return ResponseEntity.ok(response);
    }

    /**
     * Get skill group detail by ID, including skill list and total download count.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SkillGroupDetailResponse> getSkillGroupDetail(@PathVariable String id) {
        SkillGroupDetailResponse response = skillService.getSkillGroupDetail(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Edit a SkillGroup's name and/or description.
     * Only the owner (publisher) can edit the SkillGroup.
     * Returns 404 if not found, 403 if not the owner, 409 if name conflicts.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<SkillGroupDetailResponse> updateSkillGroup(
            @PathVariable String id,
            @Valid @RequestBody UpdateSkillGroupRequest request,
            Authentication authentication) {

        String userId = (String) authentication.getPrincipal();
        SkillGroupDetailResponse response = skillService.updateSkillGroup(id, request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Record a SkillGroup copy event (increment download count by 1).
     * No authentication required — consumers can copy without logging in.
     */
    @PostMapping("/{id}/copy-event")
    public ResponseEntity<Void> recordCopyEvent(@PathVariable String id) {
        skillService.recordSkillGroupCopyEvent(id);
        return ResponseEntity.ok().build();
    }
}
