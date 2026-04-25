package com.catalogcollector.dto;

import com.catalogcollector.entity.CatalogItem;
import com.catalogcollector.entity.PendingUpdate;
import com.catalogcollector.entity.UpdateStatus;
import com.catalogcollector.entity.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PendingUpdateResponseTest {

    @Test
    void from_shouldMapAllFields() {
        User user = new User("user@example.com", "User", "hash");
        user.setId(UUID.randomUUID());
        CatalogItem item = new CatalogItem("1234567890", "UPC", "Test");
        item.setId(UUID.randomUUID());

        PendingUpdate update = new PendingUpdate(
                user, item, "title", "\"Old\"", "\"New\"", "openlibrary");
        update.setId(UUID.randomUUID());
        update.setStatus(UpdateStatus.ACCEPTED);
        Instant resolvedAt = Instant.now();
        update.setResolvedAt(resolvedAt);

        PendingUpdateResponse response = PendingUpdateResponse.from(update);

        assertThat(response.id()).isEqualTo(update.getId());
        assertThat(response.userId()).isEqualTo(user.getId());
        assertThat(response.catalogItemId()).isEqualTo(item.getId());
        assertThat(response.fieldName()).isEqualTo("title");
        assertThat(response.oldValue()).isEqualTo("\"Old\"");
        assertThat(response.newValue()).isEqualTo("\"New\"");
        assertThat(response.source()).isEqualTo("openlibrary");
        assertThat(response.status()).isEqualTo(UpdateStatus.ACCEPTED);
        assertThat(response.resolvedAt()).isEqualTo(resolvedAt);
    }
}
