package com.catalogcollector.dto;

import java.util.List;

public record CursorPage<T>(
        List<T> content,
        String nextCursor,
        boolean hasMore,
        int size
) {
}
