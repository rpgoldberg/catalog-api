package com.catalogcollector.dto;

import com.catalogcollector.entity.PendingUpdate;
import com.catalogcollector.entity.UpdateStatus;

import java.time.Instant;
import java.util.UUID;

public record PendingUpdateResponse(
        UUID id,
        UUID userId,
        UUID catalogItemId,
        String fieldName,
        String oldValue,
        String newValue,
        String source,
        UpdateStatus status,
        Instant createdAt,
        Instant resolvedAt
) {
    public static PendingUpdateResponse from(PendingUpdate update) {
        return new PendingUpdateResponse(
                update.getId(),
                update.getUser().getId(),
                update.getCatalogItem().getId(),
                update.getFieldName(),
                update.getOldValue(),
                update.getNewValue(),
                update.getSource(),
                update.getStatus(),
                update.getCreatedAt(),
                update.getResolvedAt());
    }
}
