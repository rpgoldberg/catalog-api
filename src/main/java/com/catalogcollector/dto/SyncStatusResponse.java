package com.catalogcollector.dto;

import java.time.Instant;

public record SyncStatusResponse(
        Instant serverTimestamp,
        long catalogItemCount,
        long collectionEntryCount,
        long pendingReviewCount
) {
}
