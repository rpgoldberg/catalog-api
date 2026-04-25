package com.catalogcollector.repository;

import com.catalogcollector.entity.CollectionEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CollectionEntryRepository extends JpaRepository<CollectionEntry, UUID> {

    List<CollectionEntry> findByUserId(UUID userId);

    Optional<CollectionEntry> findByIdAndUserId(UUID id, UUID userId);

    Optional<CollectionEntry> findByClientId(String clientId);

    List<CollectionEntry> findByUserIdAndUpdatedAtAfterOrderByUpdatedAtAsc(
            UUID userId, Instant since);

    @Query("SELECT e FROM CollectionEntry e WHERE e.user.id = :userId " +
           "AND e.deletedAt IS NULL ORDER BY e.createdAt, e.id")
    List<CollectionEntry> findByUserIdFirstPage(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT e FROM CollectionEntry e WHERE e.user.id = :userId " +
           "AND e.deletedAt IS NULL " +
           "AND (e.createdAt > :ts OR (e.createdAt = :ts AND e.id > :id)) " +
           "ORDER BY e.createdAt, e.id")
    List<CollectionEntry> findByUserIdAfterCursor(@Param("userId") UUID userId,
                                                  @Param("ts") Instant ts,
                                                  @Param("id") UUID id,
                                                  Pageable pageable);

    long countByUserIdAndDeletedAtIsNull(UUID userId);
}
