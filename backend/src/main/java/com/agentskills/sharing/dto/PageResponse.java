package com.agentskills.sharing.dto;

import java.util.List;

/**
 * Generic paginated response wrapper.
 */
public record PageResponse<T>(
        List<T> items,
        long total,
        int page,
        int pageSize
) {
}
