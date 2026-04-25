package com.catalogcollector.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogItemTest {

    @Test
    void constructor_shouldSetBarcodeAndTitle() {
        CatalogItem item = new CatalogItem("9781234567890", "ISBN", "Manga Title");

        assertThat(item.getBarcode()).isEqualTo("9781234567890");
        assertThat(item.getBarcodeType()).isEqualTo("ISBN");
        assertThat(item.getCanonicalTitle()).isEqualTo("Manga Title");
    }

    @Test
    void setters_shouldUpdateCanonicalFields() {
        CatalogItem item = new CatalogItem();
        item.setCanonicalTitle("Title");
        item.setCanonicalPublisher("Publisher");
        item.setCanonicalPageCount(200);
        item.setCanonicalCoverImageUrl("https://img.example.com/cover.jpg");
        item.setCanonicalEdition("1st");
        item.setCanonicalLanguage("ja");
        item.setCanonicalReleaseDate("2025-01-15");

        assertThat(item.getCanonicalTitle()).isEqualTo("Title");
        assertThat(item.getCanonicalPublisher()).isEqualTo("Publisher");
        assertThat(item.getCanonicalPageCount()).isEqualTo(200);
        assertThat(item.getCanonicalCoverImageUrl()).isEqualTo("https://img.example.com/cover.jpg");
        assertThat(item.getCanonicalEdition()).isEqualTo("1st");
        assertThat(item.getCanonicalLanguage()).isEqualTo("ja");
        assertThat(item.getCanonicalReleaseDate()).isEqualTo("2025-01-15");
    }

    @Test
    void setters_shouldUpdateEnrichmentFields() {
        CatalogItem item = new CatalogItem();
        Instant now = Instant.now();

        item.setLookupSource("openlibrary");
        item.setLastEnrichedAt(now);
        item.setMediaType(MediaType.MANGA);
        item.setCanonicalMetadata(Map.of("volumes", 12, "ongoing", true));

        assertThat(item.getLookupSource()).isEqualTo("openlibrary");
        assertThat(item.getLastEnrichedAt()).isEqualTo(now);
        assertThat(item.getMediaType()).isEqualTo(MediaType.MANGA);
        assertThat(item.getCanonicalMetadata()).containsEntry("volumes", 12);
    }

    @Test
    void setters_shouldUpdateSyncFields() {
        CatalogItem item = new CatalogItem();
        UUID id = UUID.randomUUID();
        Instant deletedAt = Instant.now();

        item.setId(id);
        item.setClientId("client-uuid");
        item.setDeletedAt(deletedAt);

        assertThat(item.getId()).isEqualTo(id);
        assertThat(item.getClientId()).isEqualTo("client-uuid");
        assertThat(item.getDeletedAt()).isEqualTo(deletedAt);
    }

    @Test
    void onCreate_shouldSetTimestamps() {
        CatalogItem item = new CatalogItem();
        item.onCreate();

        assertThat(item.getCreatedAt()).isNotNull();
        assertThat(item.getUpdatedAt()).isNotNull();
    }

    @Test
    void onUpdate_shouldRefreshUpdatedAt() {
        CatalogItem item = new CatalogItem();
        item.onCreate();
        item.onUpdate();

        assertThat(item.getUpdatedAt()).isNotNull();
    }
}
