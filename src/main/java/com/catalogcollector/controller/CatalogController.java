package com.catalogcollector.controller;

import com.catalogcollector.dto.CatalogItemRequest;
import com.catalogcollector.dto.CatalogItemResponse;
import com.catalogcollector.dto.CursorPage;
import com.catalogcollector.dto.EffectiveCatalogItemResponse;
import com.catalogcollector.dto.EnrichmentResponse;
import com.catalogcollector.service.CatalogEnrichmentService;
import com.catalogcollector.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/catalog")
public class CatalogController {

    private final CatalogService catalogService;
    private final CatalogEnrichmentService enrichmentService;

    public CatalogController(CatalogService catalogService,
                             CatalogEnrichmentService enrichmentService) {
        this.catalogService = catalogService;
        this.enrichmentService = enrichmentService;
    }

    @GetMapping("/items")
    public ResponseEntity<CursorPage<CatalogItemResponse>> getItems(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(catalogService.getItems(cursor, size));
    }

    @PostMapping("/items")
    public ResponseEntity<CatalogItemResponse> createItem(
            @Valid @RequestBody CatalogItemRequest request) {
        CatalogItemResponse response = catalogService.createItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<CatalogItemResponse> getItem(@PathVariable UUID id) {
        return ResponseEntity.ok(catalogService.getItemById(id));
    }

    @GetMapping("/items/{id}/effective")
    public ResponseEntity<EffectiveCatalogItemResponse> getEffectiveView(
            Authentication authentication,
            @PathVariable UUID id) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(enrichmentService.getEffectiveView(userId, id));
    }

    @PostMapping("/items/{id}/enrich")
    public ResponseEntity<EnrichmentResponse> enrichItem(@PathVariable UUID id) {
        return ResponseEntity.ok(enrichmentService.reEnrichItem(id));
    }

    @GetMapping("/lookup/{barcode}")
    public ResponseEntity<CatalogItemResponse> lookupByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(catalogService.lookupByBarcode(barcode));
    }
}
