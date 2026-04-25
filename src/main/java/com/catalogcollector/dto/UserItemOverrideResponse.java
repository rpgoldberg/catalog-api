package com.catalogcollector.dto;

import com.catalogcollector.entity.OverridePolicy;
import com.catalogcollector.entity.UserItemOverride;

import java.time.Instant;
import java.util.UUID;

public record UserItemOverrideResponse(
        UUID id,
        UUID userId,
        UUID catalogItemId,
        String fieldName,
        String overrideValue,
        OverridePolicy policy,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserItemOverrideResponse from(UserItemOverride override) {
        return new UserItemOverrideResponse(
                override.getId(),
                override.getUser().getId(),
                override.getCatalogItem().getId(),
                override.getFieldName(),
                override.getOverrideValue(),
                override.getPolicy(),
                override.getCreatedAt(),
                override.getUpdatedAt());
    }
}
