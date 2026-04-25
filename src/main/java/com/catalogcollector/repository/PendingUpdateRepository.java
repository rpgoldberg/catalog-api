package com.catalogcollector.repository;

import com.catalogcollector.entity.PendingUpdate;
import com.catalogcollector.entity.UpdateStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PendingUpdateRepository extends JpaRepository<PendingUpdate, UUID> {

    List<PendingUpdate> findByUserIdAndStatus(UUID userId, UpdateStatus status);

    List<PendingUpdate> findByUserIdAndCatalogItemId(UUID userId, UUID catalogItemId);

    long countByUserIdAndStatus(UUID userId, UpdateStatus status);
}
