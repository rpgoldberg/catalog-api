package com.catalogcollector.controller;

import com.catalogcollector.dto.CatalogItemRequest;
import com.catalogcollector.dto.CatalogItemResponse;
import com.catalogcollector.service.CatalogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/items")
    public ResponseEntity<List<CatalogItemResponse>> getAllItems() {
        return ResponseEntity.ok(catalogService.getAllItems());
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

    @GetMapping("/lookup/{barcode}")
    public ResponseEntity<CatalogItemResponse> lookupByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(catalogService.lookupByBarcode(barcode));
    }
}
