package com.catalogcollector.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public final class CursorPaginationHelper {

    private static final String SEPARATOR = "::";

    private CursorPaginationHelper() {
    }

    public static String encode(Instant timestamp, UUID id) {
        String raw = timestamp.toString() + SEPARATOR + id.toString();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static DecodedCursor decode(String cursor) {
        byte[] decoded = Base64.getUrlDecoder().decode(cursor);
        String raw = new String(decoded, StandardCharsets.UTF_8);
        String[] parts = raw.split(SEPARATOR, 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid cursor format");
        }
        return new DecodedCursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
    }

    public record DecodedCursor(Instant timestamp, UUID id) {
    }
}
