package com.catalogcollector.service;

import com.catalogcollector.controller.GlobalExceptionHandler.ResourceNotFoundException;
import com.catalogcollector.dto.DeltaSyncResponse;
import com.catalogcollector.dto.MutationResult;
import com.catalogcollector.dto.SyncMutation;
import com.catalogcollector.dto.SyncMutation.MutationType;
import com.catalogcollector.dto.SyncPushResponse;
import com.catalogcollector.dto.SyncStatusResponse;
import com.catalogcollector.entity.CatalogItem;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

    @Mock
    private CatalogItemRepository catalogItemRepository;
    @Mock
    private CollectionEntryRepository collectionEntryRepository;
    @Mock
    private UserItemOverrideRepository userItemOverrideRepository;
    @Mock
    private PendingUpdateRepository pendingUpdateRepository;
    @Mock
    private UserRepository userRepository;

    private SyncService syncService;

    private User testUser;
    private CatalogItem testItem;

    @BeforeEach
    void setUp() {
        syncService = new SyncService(catalogItemRepository, collectionEntryRepository,
                userItemOverrideRepository, pendingUpdateRepository, userRepository);

        testUser = new User("user@example.com", "Test User", "hash");
        testUser.setId(UUID.randomUUID());

        testItem = new CatalogItem("1234567890", "UPC", "Test Item");
        testItem.setId(UUID.randomUUID());
    }

    // --- GET /sync/delta ---

    @Test
    void getDelta_shouldReturnChangedRecordsSinceTimestamp() {
        Instant since = Instant.parse("2026-01-01T00:00:00Z");

        when(catalogItemRepository.findByUpdatedAtAfterOrderByUpdatedAtAsc(since))
                .thenReturn(List.of(testItem));
        when(collectionEntryRepository.findByUserIdAndUpdatedAtAfterOrderByUpdatedAtAsc(
                testUser.getId(), since))
                .thenReturn(List.of());
        when(userItemOverrideRepository.findByUserIdAndUpdatedAtAfter(testUser.getId(), since))
                .thenReturn(List.of());
        when(pendingUpdateRepository.findByUserIdAndStatus(testUser.getId(), UpdateStatus.PENDING))
                .thenReturn(List.of());

        DeltaSyncResponse response = syncService.getDelta(testUser.getId(), since, 500);

        assertThat(response.catalogItems()).hasSize(1);
        assertThat(response.catalogItems().getFirst().barcode()).isEqualTo("1234567890");
        assertThat(response.serverTimestamp()).isNotNull();
        assertThat(response.hasMore()).isFalse();
    }

    @Test
    void getDelta_shouldIndicateHasMoreWhenLimitExceeded() {
        Instant since = Instant.parse("2026-01-01T00:00:00Z");
        CatalogItem item1 = new CatalogItem("111", "UPC", "Item 1");
        item1.setId(UUID.randomUUID());
        CatalogItem item2 = new CatalogItem("222", "UPC", "Item 2");
        item2.setId(UUID.randomUUID());
        CatalogItem item3 = new CatalogItem("333", "UPC", "Item 3");
        item3.setId(UUID.randomUUID());

        when(catalogItemRepository.findByUpdatedAtAfterOrderByUpdatedAtAsc(since))
                .thenReturn(List.of(item1, item2, item3));
        when(collectionEntryRepository.findByUserIdAndUpdatedAtAfterOrderByUpdatedAtAsc(
                testUser.getId(), since))
                .thenReturn(List.of());
        when(userItemOverrideRepository.findByUserIdAndUpdatedAtAfter(testUser.getId(), since))
                .thenReturn(List.of());
        when(pendingUpdateRepository.findByUserIdAndStatus(testUser.getId(), UpdateStatus.PENDING))
                .thenReturn(List.of());

        DeltaSyncResponse response = syncService.getDelta(testUser.getId(), since, 2);

        assertThat(response.catalogItems()).hasSize(2);
        assertThat(response.hasMore()).isTrue();
    }

    @Test
    void getDelta_shouldIncludeSoftDeletedRecords() {
        Instant since = Instant.parse("2026-01-01T00:00:00Z");
        CatalogItem deletedItem = new CatalogItem("deleted-bc", "UPC", "Deleted");
        deletedItem.setId(UUID.randomUUID());
        deletedItem.setDeletedAt(Instant.now());

        when(catalogItemRepository.findByUpdatedAtAfterOrderByUpdatedAtAsc(since))
                .thenReturn(List.of(deletedItem));
        when(collectionEntryRepository.findByUserIdAndUpdatedAtAfterOrderByUpdatedAtAsc(
                testUser.getId(), since))
                .thenReturn(List.of());
        when(userItemOverrideRepository.findByUserIdAndUpdatedAtAfter(testUser.getId(), since))
                .thenReturn(List.of());
        when(pendingUpdateRepository.findByUserIdAndStatus(testUser.getId(), UpdateStatus.PENDING))
                .thenReturn(List.of());

        DeltaSyncResponse response = syncService.getDelta(testUser.getId(), since, 500);

        assertThat(response.catalogItems()).hasSize(1);
        assertThat(response.catalogItems().getFirst().deletedAt()).isNotNull();
    }

    @Test
    void getDelta_shouldReturnUserOverridesAndPendingUpdates() {
        Instant since = Instant.parse("2026-01-01T00:00:00Z");
        UserItemOverride override = new UserItemOverride(
                testUser, testItem, "title", "\"My Title\"", OverridePolicy.LOCKED);
        override.setId(UUID.randomUUID());

        PendingUpdate pending = new PendingUpdate(
                testUser, testItem, "publisher", "\"Old\"", "\"New\"", "openlibrary");
        pending.setId(UUID.randomUUID());

        when(catalogItemRepository.findByUpdatedAtAfterOrderByUpdatedAtAsc(since))
                .thenReturn(List.of());
        when(collectionEntryRepository.findByUserIdAndUpdatedAtAfterOrderByUpdatedAtAsc(
                testUser.getId(), since))
                .thenReturn(List.of());
        when(userItemOverrideRepository.findByUserIdAndUpdatedAtAfter(testUser.getId(), since))
                .thenReturn(List.of(override));
        when(pendingUpdateRepository.findByUserIdAndStatus(testUser.getId(), UpdateStatus.PENDING))
                .thenReturn(List.of(pending));

        DeltaSyncResponse response = syncService.getDelta(testUser.getId(), since, 500);

        assertThat(response.userOverrides()).hasSize(1);
        assertThat(response.userOverrides().getFirst().fieldName()).isEqualTo("title");
        assertThat(response.pendingUpdates()).hasSize(1);
        assertThat(response.pendingUpdates().getFirst().fieldName()).isEqualTo("publisher");
    }

    // --- POST /sync/push: CREATE_ITEM ---

    @Test
    void push_createItem_shouldCreateAndReturnServerId() {
        SyncMutation mutation = new SyncMutation(MutationType.CREATE_ITEM, "client-1",
                Map.of("barcode", "9781234567890", "barcodeType", "ISBN",
                        "canonicalTitle", "Test Book", "mediaType", "MANGA"),
                Instant.now());

        CatalogItem saved = new CatalogItem("9781234567890", "ISBN", "Test Book");
        saved.setId(UUID.randomUUID());
        saved.setClientId("client-1");

        when(catalogItemRepository.findByClientId("client-1")).thenReturn(Optional.empty());
        when(catalogItemRepository.save(any(CatalogItem.class))).thenReturn(saved);

        SyncPushResponse response = syncService.push(testUser.getId(), List.of(mutation));

        assertThat(response.results()).hasSize(1);
        assertThat(response.results().getFirst().status()).isEqualTo(MutationResult.Status.CREATED);
        assertThat(response.results().getFirst().serverId()).isEqualTo(saved.getId());
        assertThat(response.results().getFirst().clientId()).isEqualTo("client-1");
    }

    @Test
    void push_createItem_shouldBeIdempotentOnDuplicateClientId() {
        CatalogItem existing = new CatalogItem("9781234567890", "ISBN", "Test Book");
        existing.setId(UUID.randomUUID());
        existing.setClientId("client-1");

        SyncMutation mutation = new SyncMutation(MutationType.CREATE_ITEM, "client-1",
                Map.of("barcode", "9781234567890", "barcodeType", "ISBN",
                        "canonicalTitle", "Test Book"),
                Instant.now());

        when(catalogItemRepository.findByClientId("client-1")).thenReturn(Optional.of(existing));

        SyncPushResponse response = syncService.push(testUser.getId(), List.of(mutation));

        assertThat(response.results()).hasSize(1);
        assertThat(response.results().getFirst().status()).isEqualTo(MutationResult.Status.CONFLICT);
        assertThat(response.results().getFirst().serverId()).isEqualTo(existing.getId());
    }

    // --- POST /sync/push: ADD_TO_COLLECTION ---

    @Test
    void push_addToCollection_shouldCreateEntryAndReturnServerId() {
        SyncMutation mutation = new SyncMutation(MutationType.ADD_TO_COLLECTION, "client-2",
                Map.of("catalogItemId", testItem.getId().toString(),
                        "condition", "Mint", "quantity", 1),
                Instant.now());

        CollectionEntry saved = new CollectionEntry(testUser, testItem);
        saved.setId(UUID.randomUUID());
        saved.setClientId("client-2");

        when(collectionEntryRepository.findByClientId("client-2")).thenReturn(Optional.empty());
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(catalogItemRepository.findById(testItem.getId())).thenReturn(Optional.of(testItem));
        when(collectionEntryRepository.save(any(CollectionEntry.class))).thenReturn(saved);

        SyncPushResponse response = syncService.push(testUser.getId(), List.of(mutation));

        assertThat(response.results()).hasSize(1);
        assertThat(response.results().getFirst().status()).isEqualTo(MutationResult.Status.CREATED);
        assertThat(response.results().getFirst().serverId()).isEqualTo(saved.getId());
    }

    @Test
    void push_addToCollection_shouldBeIdempotentOnDuplicateClientId() {
        CollectionEntry existing = new CollectionEntry(testUser, testItem);
        existing.setId(UUID.randomUUID());
        existing.setClientId("client-2");

        SyncMutation mutation = new SyncMutation(MutationType.ADD_TO_COLLECTION, "client-2",
                Map.of("catalogItemId", testItem.getId().toString()), Instant.now());

        when(collectionEntryRepository.findByClientId("client-2")).thenReturn(Optional.of(existing));

        SyncPushResponse response = syncService.push(testUser.getId(), List.of(mutation));

        assertThat(response.results().getFirst().status()).isEqualTo(MutationResult.Status.CONFLICT);
        assertThat(response.results().getFirst().serverId()).isEqualTo(existing.getId());
    }

    // --- POST /sync/push: UPDATE_COLLECTION ---

    @Test
    void push_updateCollection_shouldUpdateEntryFields() {
        CollectionEntry entry = new CollectionEntry(testUser, testItem);
        entry.setId(UUID.randomUUID());
        entry.setQuantity(1);

        SyncMutation mutation = new SyncMutation(MutationType.UPDATE_COLLECTION, "client-3",
                Map.of("entryId", entry.getId().toString(),
                        "condition", "Good", "quantity", 3),
                Instant.now());

        when(collectionEntryRepository.findByIdAndUserId(entry.getId(), testUser.getId()))
                .thenReturn(Optional.of(entry));
        when(collectionEntryRepository.save(any(CollectionEntry.class))).thenReturn(entry);

        SyncPushResponse response = syncService.push(testUser.getId(), List.of(mutation));

        assertThat(response.results().getFirst().status()).isEqualTo(MutationResult.Status.UPDATED);
        assertThat(response.results().getFirst().serverId()).isEqualTo(entry.getId());
    }

    @Test
    void push_updateCollection_shouldReturnErrorWhenEntryNotFound() {
        UUID missingId = UUID.randomUUID();
        SyncMutation mutation = new SyncMutation(MutationType.UPDATE_COLLECTION, "client-4",
                Map.of("entryId", missingId.toString(), "condition", "Good"),
                Instant.now());

        when(collectionEntryRepository.findByIdAndUserId(missingId, testUser.getId()))
                .thenReturn(Optional.empty());

        SyncPushResponse response = syncService.push(testUser.getId(), List.of(mutation));

        assertThat(response.results().getFirst().status()).isEqualTo(MutationResult.Status.ERROR);
    }

    // --- POST /sync/push: DELETE_FROM_COLLECTION ---

    @Test
    void push_deleteFromCollection_shouldSoftDeleteEntry() {
        CollectionEntry entry = new CollectionEntry(testUser, testItem);
        entry.setId(UUID.randomUUID());

        SyncMutation mutation = new SyncMutation(MutationType.DELETE_FROM_COLLECTION, "client-5",
                Map.of("entryId", entry.getId().toString()), Instant.now());

        when(collectionEntryRepository.findByIdAndUserId(entry.getId(), testUser.getId()))
                .thenReturn(Optional.of(entry));
        when(collectionEntryRepository.save(any(CollectionEntry.class))).thenReturn(entry);

        SyncPushResponse response = syncService.push(testUser.getId(), List.of(mutation));

        assertThat(response.results().getFirst().status()).isEqualTo(MutationResult.Status.DELETED);
        assertThat(entry.getDeletedAt()).isNotNull();
    }

    // --- POST /sync/push: SET_OVERRIDE ---

    @Test
    void push_setOverride_shouldCreateNewOverride() {
        SyncMutation mutation = new SyncMutation(MutationType.SET_OVERRIDE, "client-6",
                Map.of("catalogItemId", testItem.getId().toString(),
                        "fieldName", "title",
                        "overrideValue", "\"My Custom Title\"",
                        "policy", "LOCKED"),
                Instant.now());

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(catalogItemRepository.findById(testItem.getId())).thenReturn(Optional.of(testItem));
        when(userItemOverrideRepository.findByUserIdAndCatalogItemIdAndFieldName(
                testUser.getId(), testItem.getId(), "title"))
                .thenReturn(Optional.empty());

        UserItemOverride saved = new UserItemOverride(
                testUser, testItem, "title", "\"My Custom Title\"", OverridePolicy.LOCKED);
        saved.setId(UUID.randomUUID());
        when(userItemOverrideRepository.save(any(UserItemOverride.class))).thenReturn(saved);

        SyncPushResponse response = syncService.push(testUser.getId(), List.of(mutation));

        assertThat(response.results().getFirst().status()).isEqualTo(MutationResult.Status.CREATED);
        assertThat(response.results().getFirst().serverId()).isEqualTo(saved.getId());
    }

    @Test
    void push_setOverride_shouldUpdateExistingOverride() {
        UserItemOverride existing = new UserItemOverride(
                testUser, testItem, "title", "\"Old\"", OverridePolicy.REVIEW);
        existing.setId(UUID.randomUUID());

        SyncMutation mutation = new SyncMutation(MutationType.SET_OVERRIDE, "client-7",
                Map.of("catalogItemId", testItem.getId().toString(),
                        "fieldName", "title",
                        "overrideValue", "\"New\"",
                        "policy", "LOCKED"),
                Instant.now());

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(catalogItemRepository.findById(testItem.getId())).thenReturn(Optional.of(testItem));
        when(userItemOverrideRepository.findByUserIdAndCatalogItemIdAndFieldName(
                testUser.getId(), testItem.getId(), "title"))
                .thenReturn(Optional.of(existing));
        when(userItemOverrideRepository.save(any(UserItemOverride.class))).thenReturn(existing);

        SyncPushResponse response = syncService.push(testUser.getId(), List.of(mutation));

        assertThat(response.results().getFirst().status()).isEqualTo(MutationResult.Status.UPDATED);
    }

    // --- POST /sync/push: RESOLVE_PENDING ---

    @Test
    void push_resolvePending_shouldAcceptPendingUpdate() {
        PendingUpdate pending = new PendingUpdate(
                testUser, testItem, "title", "\"Old\"", "\"New\"", "openlibrary");
        pending.setId(UUID.randomUUID());

        SyncMutation mutation = new SyncMutation(MutationType.RESOLVE_PENDING, "client-8",
                Map.of("pendingUpdateId", pending.getId().toString(),
                        "resolution", "ACCEPTED"),
                Instant.now());

        when(pendingUpdateRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
        when(pendingUpdateRepository.save(any(PendingUpdate.class))).thenReturn(pending);

        SyncPushResponse response = syncService.push(testUser.getId(), List.of(mutation));

        assertThat(response.results().getFirst().status()).isEqualTo(MutationResult.Status.UPDATED);
        assertThat(pending.getStatus()).isEqualTo(UpdateStatus.ACCEPTED);
        assertThat(pending.getResolvedAt()).isNotNull();
    }

    @Test
    void push_resolvePending_shouldRejectPendingUpdate() {
        PendingUpdate pending = new PendingUpdate(
                testUser, testItem, "title", "\"Old\"", "\"New\"", "openlibrary");
        pending.setId(UUID.randomUUID());

        SyncMutation mutation = new SyncMutation(MutationType.RESOLVE_PENDING, "client-9",
                Map.of("pendingUpdateId", pending.getId().toString(),
                        "resolution", "REJECTED"),
                Instant.now());

        when(pendingUpdateRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
        when(pendingUpdateRepository.save(any(PendingUpdate.class))).thenReturn(pending);

        SyncPushResponse response = syncService.push(testUser.getId(), List.of(mutation));

        assertThat(response.results().getFirst().status()).isEqualTo(MutationResult.Status.UPDATED);
        assertThat(pending.getStatus()).isEqualTo(UpdateStatus.REJECTED);
    }

    // --- POST /sync/push: multiple mutations ---

    @Test
    void push_shouldProcessMultipleMutationsInOrder() {
        SyncMutation create = new SyncMutation(MutationType.CREATE_ITEM, "client-a",
                Map.of("barcode", "111", "barcodeType", "UPC", "canonicalTitle", "Item A"),
                Instant.now());
        SyncMutation create2 = new SyncMutation(MutationType.CREATE_ITEM, "client-b",
                Map.of("barcode", "222", "barcodeType", "UPC", "canonicalTitle", "Item B"),
                Instant.now());

        CatalogItem savedA = new CatalogItem("111", "UPC", "Item A");
        savedA.setId(UUID.randomUUID());
        savedA.setClientId("client-a");
        CatalogItem savedB = new CatalogItem("222", "UPC", "Item B");
        savedB.setId(UUID.randomUUID());
        savedB.setClientId("client-b");

        when(catalogItemRepository.findByClientId("client-a")).thenReturn(Optional.empty());
        when(catalogItemRepository.findByClientId("client-b")).thenReturn(Optional.empty());
        when(catalogItemRepository.save(any(CatalogItem.class)))
                .thenReturn(savedA).thenReturn(savedB);

        SyncPushResponse response = syncService.push(testUser.getId(), List.of(create, create2));

        assertThat(response.results()).hasSize(2);
        assertThat(response.results().get(0).clientId()).isEqualTo("client-a");
        assertThat(response.results().get(1).clientId()).isEqualTo("client-b");
    }

    // --- GET /sync/status ---

    @Test
    void getStatus_shouldReturnCounts() {
        when(catalogItemRepository.count()).thenReturn(42L);
        when(collectionEntryRepository.findByUserId(testUser.getId())).thenReturn(
                List.of(new CollectionEntry(testUser, testItem)));
        when(pendingUpdateRepository.countByUserIdAndStatus(
                testUser.getId(), UpdateStatus.PENDING)).thenReturn(3L);

        SyncStatusResponse response = syncService.getStatus(testUser.getId());

        assertThat(response.catalogItemCount()).isEqualTo(42);
        assertThat(response.collectionEntryCount()).isEqualTo(1);
        assertThat(response.pendingReviewCount()).isEqualTo(3);
        assertThat(response.serverTimestamp()).isNotNull();
    }
}
