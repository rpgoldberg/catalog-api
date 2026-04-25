package com.catalogcollector.service;

import com.catalogcollector.service.CursorPaginationHelper.DecodedCursor;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CursorPaginationHelperTest {

    @Test
    void encode_shouldProduceDecodableString() {
        Instant ts = Instant.parse("2025-06-15T10:30:00Z");
        UUID id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        String cursor = CursorPaginationHelper.encode(ts, id);

        assertThat(cursor).isNotBlank();
        DecodedCursor decoded = CursorPaginationHelper.decode(cursor);
        assertThat(decoded.timestamp()).isEqualTo(ts);
        assertThat(decoded.id()).isEqualTo(id);
    }

    @Test
    void encode_shouldUseUrlSafeBase64() {
        Instant ts = Instant.now();
        UUID id = UUID.randomUUID();

        String cursor = CursorPaginationHelper.encode(ts, id);

        // URL-safe Base64 should not contain + or /
        assertThat(cursor).doesNotContain("+").doesNotContain("/").doesNotContain("=");
    }

    @Test
    void decode_shouldRejectInvalidCursor() {
        // Base64-encode a string without the separator
        String invalid = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("noseparator".getBytes());

        assertThatThrownBy(() -> CursorPaginationHelper.decode(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid cursor format");
    }

    @Test
    void roundTrip_shouldPreserveNanoTimestamp() {
        Instant ts = Instant.parse("2025-01-01T00:00:00.123456789Z");
        UUID id = UUID.randomUUID();

        String cursor = CursorPaginationHelper.encode(ts, id);
        DecodedCursor decoded = CursorPaginationHelper.decode(cursor);

        assertThat(decoded.timestamp()).isEqualTo(ts);
        assertThat(decoded.id()).isEqualTo(id);
    }
}
