package com.catalogcollector.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserItemOverrideTest {

    @Test
    void constructor_shouldSetAllFields() {
        User user = new User("user@example.com", "User", "hash");
        user.setId(UUID.randomUUID());
        CatalogItem item = new CatalogItem("1234567890", "UPC", "Test");
        item.setId(UUID.randomUUID());

        UserItemOverride override = new UserItemOverride(
                user, item, "title", "\"Custom Title\"", OverridePolicy.LOCKED);

        assertThat(override.getUser()).isEqualTo(user);
        assertThat(override.getCatalogItem()).isEqualTo(item);
        assertThat(override.getFieldName()).isEqualTo("title");
        assertThat(override.getOverrideValue()).isEqualTo("\"Custom Title\"");
        assertThat(override.getPolicy()).isEqualTo(OverridePolicy.LOCKED);
    }

    @Test
    void defaultConstructor_shouldSetDefaultPolicy() {
        UserItemOverride override = new UserItemOverride();
        assertThat(override.getPolicy()).isEqualTo(OverridePolicy.AUTO_ACCEPT);
    }

    @Test
    void setters_shouldUpdateFields() {
        UserItemOverride override = new UserItemOverride();
        UUID id = UUID.randomUUID();
        User user = new User("user@example.com", "User", "hash");
        CatalogItem item = new CatalogItem("1234567890", "UPC", "Test");

        override.setId(id);
        override.setUser(user);
        override.setCatalogItem(item);
        override.setFieldName("publisher");
        override.setOverrideValue("\"My Publisher\"");
        override.setPolicy(OverridePolicy.REVIEW);

        assertThat(override.getId()).isEqualTo(id);
        assertThat(override.getUser()).isEqualTo(user);
        assertThat(override.getCatalogItem()).isEqualTo(item);
        assertThat(override.getFieldName()).isEqualTo("publisher");
        assertThat(override.getOverrideValue()).isEqualTo("\"My Publisher\"");
        assertThat(override.getPolicy()).isEqualTo(OverridePolicy.REVIEW);
    }

    @Test
    void onCreate_shouldSetTimestamps() {
        UserItemOverride override = new UserItemOverride();
        override.onCreate();

        assertThat(override.getCreatedAt()).isNotNull();
        assertThat(override.getUpdatedAt()).isNotNull();
    }

    @Test
    void onUpdate_shouldRefreshUpdatedAt() {
        UserItemOverride override = new UserItemOverride();
        override.onCreate();
        var initial = override.getUpdatedAt();
        override.onUpdate();

        assertThat(override.getUpdatedAt()).isNotNull();
    }
}
