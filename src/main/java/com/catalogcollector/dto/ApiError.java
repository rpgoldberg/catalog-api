package com.catalogcollector.dto;

import java.time.Instant;
import java.util.List;

public record ApiError(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp,
        List<FieldError> fieldErrors
) {
    public ApiError(int status, String error, String message, String path) {
        this(status, error, message, path, Instant.now(), null);
    }

    public ApiError(int status, String error, String message, String path, List<FieldError> fieldErrors) {
        this(status, error, message, path, Instant.now(), fieldErrors);
    }

    public record FieldError(String field, String message) {
    }
}
