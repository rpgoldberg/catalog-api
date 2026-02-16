package com.catalogcollector.repository;

import com.catalogcollector.entity.CollectionEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CollectionEntryRepository extends JpaRepository<CollectionEntry, UUID> {

    List<CollectionEntry> findByUserId(UUID userId);

    Optional<CollectionEntry> findByIdAndUserId(UUID id, UUID userId);
}
