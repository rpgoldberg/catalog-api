package com.catalogcollector.service;

import com.catalogcollector.controller.GlobalExceptionHandler.ResourceNotFoundException;
import com.catalogcollector.dto.CollectionEntryRequest;
import com.catalogcollector.dto.CollectionEntryResponse;
import com.catalogcollector.entity.CatalogItem;
import com.catalogcollector.entity.CollectionEntry;
import com.catalogcollector.entity.User;
import com.catalogcollector.repository.CatalogItemRepository;
import com.catalogcollector.repository.CollectionEntryRepository;
import com.catalogcollector.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CollectionService {

    private final CollectionEntryRepository collectionEntryRepository;
    private final UserRepository userRepository;
    private final CatalogItemRepository catalogItemRepository;

    public CollectionService(CollectionEntryRepository collectionEntryRepository,
                             UserRepository userRepository,
                             CatalogItemRepository catalogItemRepository) {
        this.collectionEntryRepository = collectionEntryRepository;
        this.userRepository = userRepository;
        this.catalogItemRepository = catalogItemRepository;
    }

    @Transactional(readOnly = true)
    public List<CollectionEntryResponse> getUserCollection(UUID userId) {
        return collectionEntryRepository.findByUserId(userId).stream()
                .map(CollectionEntryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CollectionEntryResponse getEntry(UUID entryId, UUID userId) {
        CollectionEntry entry = collectionEntryRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection entry not found: " + entryId));
        return CollectionEntryResponse.from(entry);
    }

    @Transactional
    public CollectionEntryResponse createEntry(UUID userId, CollectionEntryRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        CatalogItem catalogItem = catalogItemRepository.findById(request.catalogItemId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Catalog item not found: " + request.catalogItemId()));

        CollectionEntry entry = new CollectionEntry(user, catalogItem);
        entry.setCondition(request.condition());
        entry.setPurchasePrice(request.purchasePrice());
        entry.setPurchaseDate(request.purchaseDate());
        entry.setNotes(request.notes());
        entry.setQuantity(request.quantity() != null ? request.quantity() : 1);

        entry = collectionEntryRepository.save(entry);
        return CollectionEntryResponse.from(entry);
    }

    @Transactional
    public CollectionEntryResponse updateEntry(UUID entryId, UUID userId,
                                               CollectionEntryRequest request) {
        CollectionEntry entry = collectionEntryRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection entry not found: " + entryId));

        entry.setCondition(request.condition());
        entry.setPurchasePrice(request.purchasePrice());
        entry.setPurchaseDate(request.purchaseDate());
        entry.setNotes(request.notes());
        entry.setQuantity(request.quantity() != null ? request.quantity() : entry.getQuantity());

        entry = collectionEntryRepository.save(entry);
        return CollectionEntryResponse.from(entry);
    }

    @Transactional
    public void deleteEntry(UUID entryId, UUID userId) {
        CollectionEntry entry = collectionEntryRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection entry not found: " + entryId));
        collectionEntryRepository.delete(entry);
    }
}
