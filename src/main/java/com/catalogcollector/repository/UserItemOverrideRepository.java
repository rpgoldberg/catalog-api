package com.catalogcollector.repository;

import com.catalogcollector.entity.UserItemOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserItemOverrideRepository extends JpaRepository<UserItemOverride, UUID> {

    List<UserItemOverride> findByUserId(UUID userId);

    List<UserItemOverride> findByUserIdAndCatalogItemId(UUID userId, UUID catalogItemId);

    Optional<UserItemOverride> findByUserIdAndCatalogItemIdAndFieldName(
            UUID userId, UUID catalogItemId, String fieldName);

    List<UserItemOverride> findByUserIdAndUpdatedAtAfter(UUID userId, Instant since);

    List<UserItemOverride> findByCatalogItemIdAndFieldName(UUID catalogItemId, String fieldName);
}
