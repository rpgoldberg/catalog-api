package com.catalogcollector.dto;

import java.util.List;
import java.util.UUID;

public record EnrichmentResponse(
        UUID catalogItemId,
        String lookupSource,
        List<FieldChange> changes,
        int pendingReviewsCreated
) {

    public record FieldChange(String fieldName, String oldValue, String newValue) {
    }
}
