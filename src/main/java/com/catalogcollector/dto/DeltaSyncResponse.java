package com.catalogcollector.dto;

import java.time.Instant;
import java.util.List;

public record DeltaSyncResponse(
        List<CatalogItemResponse> catalogItems,
        List<CollectionEntryResponse> collectionEntries,
        List<UserItemOverrideResponse> userOverrides,
        List<PendingUpdateResponse> pendingUpdates,
        Instant serverTimestamp,
        boolean hasMore
) {
}
