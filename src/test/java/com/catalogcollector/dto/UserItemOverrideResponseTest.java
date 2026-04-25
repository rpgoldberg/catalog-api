package com.catalogcollector.dto;

import com.catalogcollector.entity.CatalogItem;
import com.catalogcollector.entity.OverridePolicy;
import com.catalogcollector.entity.User;
import com.catalogcollector.entity.UserItemOverride;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserItemOverrideResponseTest {

    @Test
    void from_shouldMapAllFields() {
        User user = new User("user@example.com", "User", "hash");
        user.setId(UUID.randomUUID());
        CatalogItem item = new CatalogItem("1234567890", "UPC", "Test");
        item.setId(UUID.randomUUID());

        UserItemOverride override = new UserItemOverride(
                user, item, "title", "\"My Title\"", OverridePolicy.LOCKED);
        override.setId(UUID.randomUUID());

        UserItemOverrideResponse response = UserItemOverrideResponse.from(override);

        assertThat(response.id()).isEqualTo(override.getId());
        assertThat(response.userId()).isEqualTo(user.getId());
        assertThat(response.catalogItemId()).isEqualTo(item.getId());
        assertThat(response.fieldName()).isEqualTo("title");
        assertThat(response.overrideValue()).isEqualTo("\"My Title\"");
        assertThat(response.policy()).isEqualTo(OverridePolicy.LOCKED);
    }
}
