package com.catalogcollector.repository;

import com.catalogcollector.entity.CatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, UUID> {

    Optional<CatalogItem> findByBarcode(String barcode);
}
