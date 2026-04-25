package com.catalogcollector.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CursorPageTest {

    @Test
    void shouldCreatePageWithContent() {
        CursorPage<String> page = new CursorPage<>(
                List.of("a", "b"), "next-cursor", true, 2);

        assertThat(page.content()).containsExactly("a", "b");
        assertThat(page.nextCursor()).isEqualTo("next-cursor");
        assertThat(page.hasMore()).isTrue();
        assertThat(page.size()).isEqualTo(2);
    }

    @Test
    void shouldCreateEmptyPage() {
        CursorPage<String> page = new CursorPage<>(
                List.of(), null, false, 0);

        assertThat(page.content()).isEmpty();
        assertThat(page.nextCursor()).isNull();
        assertThat(page.hasMore()).isFalse();
        assertThat(page.size()).isZero();
    }
}
