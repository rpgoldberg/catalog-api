package com.catalogcollector.dto;

import java.util.UUID;

public record MutationResult(
        String clientId,
        Status status,
        UUID serverId,
        String message
) {

    public enum Status {
        CREATED,
        UPDATED,
        DELETED,
        CONFLICT,
        ERROR
    }
}
