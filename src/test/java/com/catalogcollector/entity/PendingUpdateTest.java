package com.catalogcollector.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PendingUpdateTest {

    @Test
    void constructor_shouldSetAllFields() {
        User user = new User("user@example.com", "User", "hash");
        user.setId(UUID.randomUUID());
        CatalogItem item = new CatalogItem("1234567890", "UPC", "Test");
        item.setId(UUID.randomUUID());

        PendingUpdate update = new PendingUpdate(
                user, item, "title", "\"Old Title\"", "\"New Title\"", "openlibrary");

        assertThat(update.getUser()).isEqualTo(user);
        assertThat(update.getCatalogItem()).isEqualTo(item);
        assertThat(update.getFieldName()).isEqualTo("title");
        assertThat(update.getOldValue()).isEqualTo("\"Old Title\"");
        assertThat(update.getNewValue()).isEqualTo("\"New Title\"");
        assertThat(update.getSource()).isEqualTo("openlibrary");
        assertThat(update.getStatus()).isEqualTo(UpdateStatus.PENDING);
    }

    @Test
    void defaultConstructor_shouldSetDefaultStatus() {
        PendingUpdate update = new PendingUpdate();
        assertThat(update.getStatus()).isEqualTo(UpdateStatus.PENDING);
    }

    @Test
    void setters_shouldUpdateFields() {
        PendingUpdate update = new PendingUpdate();
        UUID id = UUID.randomUUID();
        User user = new User("user@example.com", "User", "hash");
        CatalogItem item = new CatalogItem("1234567890", "UPC", "Test");
        Instant resolvedAt = Instant.now();

        update.setId(id);
        update.setUser(user);
        update.setCatalogItem(item);
        update.setFieldName("pageCount");
        update.setOldValue("200");
        update.setNewValue("210");
        update.setSource("googlebooks");
        update.setStatus(UpdateStatus.ACCEPTED);
        update.setResolvedAt(resolvedAt);

        assertThat(update.getId()).isEqualTo(id);
        assertThat(update.getUser()).isEqualTo(user);
        assertThat(update.getCatalogItem()).isEqualTo(item);
        assertThat(update.getFieldName()).isEqualTo("pageCount");
        assertThat(update.getOldValue()).isEqualTo("200");
        assertThat(update.getNewValue()).isEqualTo("210");
        assertThat(update.getSource()).isEqualTo("googlebooks");
        assertThat(update.getStatus()).isEqualTo(UpdateStatus.ACCEPTED);
        assertThat(update.getResolvedAt()).isEqualTo(resolvedAt);
    }

    @Test
    void onCreate_shouldSetCreatedAt() {
        PendingUpdate update = new PendingUpdate();
        update.onCreate();

        assertThat(update.getCreatedAt()).isNotNull();
    }
}
