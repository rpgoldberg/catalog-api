package com.catalogcollector.controller;

import com.catalogcollector.dto.CollectionEntryRequest;
import com.catalogcollector.dto.CollectionEntryResponse;
import com.catalogcollector.dto.CursorPage;
import com.catalogcollector.service.CollectionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/collections")
public class CollectionController {

    private final CollectionService collectionService;

    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @GetMapping
    public ResponseEntity<CursorPage<CollectionEntryResponse>> getUserCollection(
            Authentication authentication,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(collectionService.getUserCollectionPaged(userId, cursor, size));
    }

    @PostMapping
    public ResponseEntity<CollectionEntryResponse> createEntry(
            Authentication authentication,
            @Valid @RequestBody CollectionEntryRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        CollectionEntryResponse response = collectionService.createEntry(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CollectionEntryResponse> getEntry(
            Authentication authentication,
            @PathVariable UUID id) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(collectionService.getEntry(id, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CollectionEntryResponse> updateEntry(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody CollectionEntryRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(collectionService.updateEntry(id, userId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEntry(
            Authentication authentication,
            @PathVariable UUID id) {
        UUID userId = (UUID) authentication.getPrincipal();
        collectionService.deleteEntry(id, userId);
        return ResponseEntity.noContent().build();
    }
}
