package com.catalogcollector.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void constructor_shouldSetFields() {
        User user = new User("user@example.com", "Display Name", "hash");

        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(user.getDisplayName()).isEqualTo("Display Name");
        assertThat(user.getPasswordHash()).isEqualTo("hash");
    }

    @Test
    void setters_shouldUpdateFields() {
        User user = new User();
        UUID id = UUID.randomUUID();
        Instant deletedAt = Instant.now();

        user.setId(id);
        user.setEmail("test@example.com");
        user.setDisplayName("Test");
        user.setPasswordHash("newhash");
        user.setSubscriptionTier(User.SubscriptionTier.PREMIUM);
        user.setDeletedAt(deletedAt);

        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        assertThat(user.getDisplayName()).isEqualTo("Test");
        assertThat(user.getPasswordHash()).isEqualTo("newhash");
        assertThat(user.getSubscriptionTier()).isEqualTo(User.SubscriptionTier.PREMIUM);
        assertThat(user.getDeletedAt()).isEqualTo(deletedAt);
    }

    @Test
    void onCreate_shouldSetTimestamps() {
        User user = new User();
        user.onCreate();

        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
    }

    @Test
    void onUpdate_shouldRefreshUpdatedAt() {
        User user = new User();
        user.onCreate();
        user.onUpdate();

        assertThat(user.getUpdatedAt()).isNotNull();
    }
}
