package com.catalogcollector.repository;

import com.catalogcollector.entity.CatalogItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, UUID> {

    Optional<CatalogItem> findByBarcode(String barcode);

    Optional<CatalogItem> findByClientId(String clientId);

    List<CatalogItem> findByUpdatedAtAfterOrderByUpdatedAtAsc(Instant since);

    @Query("SELECT c FROM CatalogItem c WHERE c.deletedAt IS NULL " +
           "ORDER BY c.createdAt, c.id")
    List<CatalogItem> findFirstPage(Pageable pageable);

    @Query("SELECT c FROM CatalogItem c WHERE c.deletedAt IS NULL " +
           "AND (c.createdAt > :ts OR (c.createdAt = :ts AND c.id > :id)) " +
           "ORDER BY c.createdAt, c.id")
    List<CatalogItem> findAfterCursor(@Param("ts") Instant ts, @Param("id") UUID id,
                                      Pageable pageable);
}
