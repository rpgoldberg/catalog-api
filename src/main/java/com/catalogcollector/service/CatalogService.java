package com.catalogcollector.service;

import com.catalogcollector.controller.GlobalExceptionHandler.ResourceNotFoundException;
import com.catalogcollector.dto.CatalogItemRequest;
import com.catalogcollector.dto.CatalogItemResponse;
import com.catalogcollector.entity.CatalogItem;
import com.catalogcollector.repository.CatalogItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CatalogService {

    private final CatalogItemRepository catalogItemRepository;

    public CatalogService(CatalogItemRepository catalogItemRepository) {
        this.catalogItemRepository = catalogItemRepository;
    }

    @Transactional(readOnly = true)
    public List<CatalogItemResponse> getAllItems() {
        return catalogItemRepository.findAll().stream()
                .map(CatalogItemResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CatalogItemResponse getItemById(UUID id) {
        CatalogItem item = catalogItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catalog item not found: " + id));
        return CatalogItemResponse.from(item);
    }

    @Transactional(readOnly = true)
    public CatalogItemResponse lookupByBarcode(String barcode) {
        CatalogItem item = catalogItemRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("No item found for barcode: " + barcode));
        return CatalogItemResponse.from(item);
    }

    @Transactional
    public CatalogItemResponse createItem(CatalogItemRequest request) {
        CatalogItem item = new CatalogItem(request.barcode(), request.barcodeType(), request.name());
        item.setBrand(request.brand());
        item.setCategory(request.category());
        item.setDescription(request.description());
        item.setImageUrl(request.imageUrl());
        item.setMetadata(request.metadata());

        item = catalogItemRepository.save(item);
        return CatalogItemResponse.from(item);
    }
}
