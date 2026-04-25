package com.catalogcollector.service;

import com.catalogcollector.controller.GlobalExceptionHandler.ResourceNotFoundException;
import com.catalogcollector.dto.CatalogItemResponse;
import com.catalogcollector.dto.CollectionEntryResponse;
import com.catalogcollector.dto.DeltaSyncResponse;
import com.catalogcollector.dto.MutationResult;
import com.catalogcollector.dto.PendingUpdateResponse;
import com.catalogcollector.dto.SyncMutation;
import com.catalogcollector.dto.SyncPushResponse;
import com.catalogcollector.dto.SyncStatusResponse;
import com.catalogcollector.dto.UserItemOverrideResponse;
import com.catalogcollector.entity.CatalogItem;
import com.catalogcollector.util.BarcodeClassifier;
import com.catalogcollector.entity.CollectionEntry;
import com.catalogcollector.entity.MediaType;
import com.catalogcollector.entity.OverridePolicy;
import com.catalogcollector.entity.PendingUpdate;
import com.catalogcollector.entity.UpdateStatus;
import com.catalogcollector.entity.User;
import com.catalogcollector.entity.UserItemOverride;
import com.catalogcollector.repository.CatalogItemRepository;
import com.catalogcollector.repository.CollectionEntryRepository;
import com.catalogcollector.repository.PendingUpdateRepository;
import com.catalogcollector.repository.UserItemOverrideRepository;
import com.catalogcollector.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class SyncService {

    private final CatalogItemRepository catalogItemRepository;
    private final CollectionEntryRepository collectionEntryRepository;
    private final UserItemOverrideRepository userItemOverrideRepository;
    private final PendingUpdateRepository pendingUpdateRepository;
    private final UserRepository userRepository;

    public SyncService(CatalogItemRepository catalogItemRepository,
                       CollectionEntryRepository collectionEntryRepository,
                       UserItemOverrideRepository userItemOverrideRepository,
                       PendingUpdateRepository pendingUpdateRepository,
                       UserRepository userRepository) {
        this.catalogItemRepository = catalogItemRepository;
        this.collectionEntryRepository = collectionEntryRepository;
        this.userItemOverrideRepository = userItemOverrideRepository;
        this.pendingUpdateRepository = pendingUpdateRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public DeltaSyncResponse getDelta(UUID userId, Instant since, int limit) {
        List<CatalogItem> catalogItems =
                catalogItemRepository.findByUpdatedAtAfterOrderByUpdatedAtAsc(since);
        List<CollectionEntry> collectionEntries =
                collectionEntryRepository.findByUserIdAndUpdatedAtAfterOrderByUpdatedAtAsc(
                        userId, since);
        List<UserItemOverride> overrides =
                userItemOverrideRepository.findByUserIdAndUpdatedAtAfter(userId, since);
        List<PendingUpdate> pending =
                pendingUpdateRepository.findByUserIdAndStatus(userId, UpdateStatus.PENDING);

        boolean hasMore = catalogItems.size() > limit
                || collectionEntries.size() > limit
                || overrides.size() > limit;

        List<CatalogItemResponse> catalogItemDtos = catalogItems.stream()
                .limit(limit)
                .map(CatalogItemResponse::from)
                .toList();
        List<CollectionEntryResponse> collectionEntryDtos = collectionEntries.stream()
                .limit(limit)
                .map(CollectionEntryResponse::from)
                .toList();
        List<UserItemOverrideResponse> overrideDtos = overrides.stream()
                .limit(limit)
                .map(UserItemOverrideResponse::from)
                .toList();
        List<PendingUpdateResponse> pendingDtos = pending.stream()
                .map(PendingUpdateResponse::from)
                .toList();

        return new DeltaSyncResponse(catalogItemDtos, collectionEntryDtos,
                overrideDtos, pendingDtos, Instant.now(), hasMore);
    }

    @Transactional
    public SyncPushResponse push(UUID userId, List<SyncMutation> mutations) {
        List<MutationResult> results = new ArrayList<>();
        for (SyncMutation mutation : mutations) {
            results.add(processMutation(userId, mutation));
        }
        return new SyncPushResponse(results, Instant.now());
    }

    @Transactional(readOnly = true)
    public SyncStatusResponse getStatus(UUID userId) {
        long catalogItemCount = catalogItemRepository.count();
        long collectionEntryCount = collectionEntryRepository.findByUserId(userId).size();
        long pendingReviewCount = pendingUpdateRepository.countByUserIdAndStatus(
                userId, UpdateStatus.PENDING);
        return new SyncStatusResponse(Instant.now(), catalogItemCount,
                collectionEntryCount, pendingReviewCount);
    }

    private MutationResult processMutation(UUID userId, SyncMutation mutation) {
        try {
            return switch (mutation.type()) {
                case CREATE_ITEM -> processCreateItem(mutation);
                case ADD_TO_COLLECTION -> processAddToCollection(userId, mutation);
                case UPDATE_COLLECTION -> processUpdateCollection(userId, mutation);
                case DELETE_FROM_COLLECTION -> processDeleteFromCollection(userId, mutation);
                case SET_OVERRIDE -> processSetOverride(userId, mutation);
                case RESOLVE_PENDING -> processResolvePending(userId, mutation);
            };
        } catch (Exception e) {
            return new MutationResult(mutation.clientId(), MutationResult.Status.ERROR,
                    null, e.getMessage());
        }
    }

    private MutationResult processCreateItem(SyncMutation mutation) {
        Optional<CatalogItem> existing = catalogItemRepository.findByClientId(mutation.clientId());
        if (existing.isPresent()) {
            return new MutationResult(mutation.clientId(), MutationResult.Status.CONFLICT,
                    existing.get().getId(), "Item already exists with this clientId");
        }

        Map<String, Object> data = mutation.data();
        String barcode = getString(data, "barcode");
        String barcodeType = getString(data, "barcodeType");
        if ("UNKNOWN".equalsIgnoreCase(barcodeType) || barcodeType == null || barcodeType.isBlank()) {
            barcodeType = BarcodeClassifier.classify(barcode);
        }
        CatalogItem item = new CatalogItem(
                barcode,
                barcodeType,
                getString(data, "canonicalTitle"));
        item.setClientId(mutation.clientId());
        item.setCanonicalPublisher(getString(data, "canonicalPublisher"));
        item.setCanonicalCoverImageUrl(getString(data, "canonicalCoverImageUrl"));
        item.setCanonicalEdition(getString(data, "canonicalEdition"));
        item.setCanonicalLanguage(getString(data, "canonicalLanguage"));
        item.setCanonicalReleaseDate(getString(data, "canonicalReleaseDate"));

        if (data.containsKey("canonicalPageCount") && data.get("canonicalPageCount") != null) {
            item.setCanonicalPageCount(((Number) data.get("canonicalPageCount")).intValue());
        }
        if (data.containsKey("mediaType") && data.get("mediaType") != null) {
            item.setMediaType(MediaType.valueOf(data.get("mediaType").toString()));
        }

        item = catalogItemRepository.save(item);
        return new MutationResult(mutation.clientId(), MutationResult.Status.CREATED,
                item.getId(), null);
    }

    private MutationResult processAddToCollection(UUID userId, SyncMutation mutation) {
        Optional<CollectionEntry> existing =
                collectionEntryRepository.findByClientId(mutation.clientId());
        if (existing.isPresent()) {
            return new MutationResult(mutation.clientId(), MutationResult.Status.CONFLICT,
                    existing.get().getId(), "Entry already exists with this clientId");
        }

        Map<String, Object> data = mutation.data();
        UUID catalogItemId = UUID.fromString(getString(data, "catalogItemId"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        CatalogItem catalogItem = catalogItemRepository.findById(catalogItemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Catalog item not found: " + catalogItemId));

        CollectionEntry entry = new CollectionEntry(user, catalogItem);
        entry.setClientId(mutation.clientId());
        entry.setCondition(getString(data, "condition"));
        entry.setNotes(getString(data, "notes"));

        if (data.containsKey("quantity") && data.get("quantity") != null) {
            entry.setQuantity(((Number) data.get("quantity")).intValue());
        }
        if (data.containsKey("purchasePrice") && data.get("purchasePrice") != null) {
            entry.setPurchasePrice(new BigDecimal(data.get("purchasePrice").toString()));
        }
        if (data.containsKey("purchaseDate") && data.get("purchaseDate") != null) {
            entry.setPurchaseDate(LocalDate.parse(data.get("purchaseDate").toString()));
        }

        entry = collectionEntryRepository.save(entry);
        return new MutationResult(mutation.clientId(), MutationResult.Status.CREATED,
                entry.getId(), null);
    }

    private MutationResult processUpdateCollection(UUID userId, SyncMutation mutation) {
        Map<String, Object> data = mutation.data();
        UUID entryId = UUID.fromString(getString(data, "entryId"));

        Optional<CollectionEntry> found =
                collectionEntryRepository.findByIdAndUserId(entryId, userId);
        if (found.isEmpty()) {
            return new MutationResult(mutation.clientId(), MutationResult.Status.ERROR,
                    null, "Collection entry not found: " + entryId);
        }

        CollectionEntry entry = found.get();
        if (data.containsKey("condition")) {
            entry.setCondition(getString(data, "condition"));
        }
        if (data.containsKey("notes")) {
            entry.setNotes(getString(data, "notes"));
        }
        if (data.containsKey("quantity") && data.get("quantity") != null) {
            entry.setQuantity(((Number) data.get("quantity")).intValue());
        }
        if (data.containsKey("purchasePrice") && data.get("purchasePrice") != null) {
            entry.setPurchasePrice(new BigDecimal(data.get("purchasePrice").toString()));
        }
        if (data.containsKey("purchaseDate") && data.get("purchaseDate") != null) {
            entry.setPurchaseDate(LocalDate.parse(data.get("purchaseDate").toString()));
        }

        collectionEntryRepository.save(entry);
        return new MutationResult(mutation.clientId(), MutationResult.Status.UPDATED,
                entry.getId(), null);
    }

    private MutationResult processDeleteFromCollection(UUID userId, SyncMutation mutation) {
        Map<String, Object> data = mutation.data();
        UUID entryId = UUID.fromString(getString(data, "entryId"));

        Optional<CollectionEntry> found =
                collectionEntryRepository.findByIdAndUserId(entryId, userId);
        if (found.isEmpty()) {
            return new MutationResult(mutation.clientId(), MutationResult.Status.ERROR,
                    null, "Collection entry not found: " + entryId);
        }

        CollectionEntry entry = found.get();
        entry.setDeletedAt(Instant.now());
        collectionEntryRepository.save(entry);
        return new MutationResult(mutation.clientId(), MutationResult.Status.DELETED,
                entry.getId(), null);
    }

    private MutationResult processSetOverride(UUID userId, SyncMutation mutation) {
        Map<String, Object> data = mutation.data();
        UUID catalogItemId = UUID.fromString(getString(data, "catalogItemId"));
        String fieldName = getString(data, "fieldName");
        String overrideValue = getString(data, "overrideValue");
        OverridePolicy policy = OverridePolicy.valueOf(getString(data, "policy"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        CatalogItem catalogItem = catalogItemRepository.findById(catalogItemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Catalog item not found: " + catalogItemId));

        Optional<UserItemOverride> existing =
                userItemOverrideRepository.findByUserIdAndCatalogItemIdAndFieldName(
                        userId, catalogItemId, fieldName);

        UserItemOverride override;
        MutationResult.Status status;
        if (existing.isPresent()) {
            override = existing.get();
            override.setOverrideValue(overrideValue);
            override.setPolicy(policy);
            status = MutationResult.Status.UPDATED;
        } else {
            override = new UserItemOverride(user, catalogItem, fieldName, overrideValue, policy);
            status = MutationResult.Status.CREATED;
        }

        override = userItemOverrideRepository.save(override);
        return new MutationResult(mutation.clientId(), status, override.getId(), null);
    }

    private MutationResult processResolvePending(UUID userId, SyncMutation mutation) {
        Map<String, Object> data = mutation.data();
        UUID pendingUpdateId = UUID.fromString(getString(data, "pendingUpdateId"));
        String resolution = getString(data, "resolution");

        PendingUpdate pending = pendingUpdateRepository.findById(pendingUpdateId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pending update not found: " + pendingUpdateId));

        pending.setStatus(UpdateStatus.valueOf(resolution));
        pending.setResolvedAt(Instant.now());
        pendingUpdateRepository.save(pending);

        return new MutationResult(mutation.clientId(), MutationResult.Status.UPDATED,
                pending.getId(), null);
    }

    private static String getString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }
}
