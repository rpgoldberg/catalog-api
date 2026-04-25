package com.catalogcollector.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

public record SyncMutation(
        @NotNull MutationType type,
        @NotBlank String clientId,
        Map<String, Object> data,
        Instant clientTimestamp
) {

    public enum MutationType {
        CREATE_ITEM,
        ADD_TO_COLLECTION,
        UPDATE_COLLECTION,
        DELETE_FROM_COLLECTION,
        SET_OVERRIDE,
        RESOLVE_PENDING
    }
}
