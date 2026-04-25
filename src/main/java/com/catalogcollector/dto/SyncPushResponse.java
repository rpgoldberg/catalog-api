package com.catalogcollector.dto;

import java.time.Instant;
import java.util.List;

public record SyncPushResponse(
        List<MutationResult> results,
        Instant serverTimestamp
) {
}
