package com.catalogcollector.service;

import com.catalogcollector.controller.GlobalExceptionHandler.ResourceNotFoundException;
import com.catalogcollector.dto.CollectionEntryRequest;
import com.catalogcollector.dto.CollectionEntryResponse;
import com.catalogcollector.dto.CursorPage;
import com.catalogcollector.entity.CatalogItem;
import com.catalogcollector.entity.CollectionEntry;
import com.catalogcollector.entity.User;
import com.catalogcollector.repository.CatalogItemRepository;
import com.catalogcollector.repository.CollectionEntryRepository;
import com.catalogcollector.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionServiceTest {

    @Mock
    private CollectionEntryRepository collectionEntryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CatalogItemRepository catalogItemRepository;

    private CollectionService collectionService;

    private User testUser;
    private CatalogItem testItem;

    @BeforeEach
    void setUp() {
        collectionService = new CollectionService(
                collectionEntryRepository, userRepository, catalogItemRepository);

        testUser = new User("user@example.com", "Test User", "hash");
        testUser.setId(UUID.randomUUID());

        testItem = new CatalogItem("1234567890", "UPC", "Test Figure");
        testItem.setId(UUID.randomUUID());
    }

    @Test
    void getUserCollection_shouldReturnEntriesForUser() {
        CollectionEntry entry = createEntry();
        when(collectionEntryRepository.findByUserId(testUser.getId()))
                .thenReturn(List.of(entry));

        List<CollectionEntryResponse> results =
                collectionService.getUserCollection(testUser.getId());

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().catalogItemTitle()).isEqualTo("Test Figure");
    }

    @Test
    void getEntry_shouldReturnEntryOwnedByUser() {
        CollectionEntry entry = createEntry();
        when(collectionEntryRepository.findByIdAndUserId(entry.getId(), testUser.getId()))
                .thenReturn(Optional.of(entry));

        CollectionEntryResponse response =
                collectionService.getEntry(entry.getId(), testUser.getId());

        assertThat(response.catalogItemTitle()).isEqualTo("Test Figure");
    }

    @Test
    void getEntry_shouldThrowWhenNotFound() {
        UUID entryId = UUID.randomUUID();
        when(collectionEntryRepository.findByIdAndUserId(entryId, testUser.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> collectionService.getEntry(entryId, testUser.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createEntry_shouldPersistAndReturn() {
        CollectionEntryRequest request = new CollectionEntryRequest(
                testItem.getId(), "Mint", new BigDecimal("29.99"),
                LocalDate.of(2025, 1, 15), "Great find!", 2);

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(catalogItemRepository.findById(testItem.getId())).thenReturn(Optional.of(testItem));

        CollectionEntry savedEntry = createEntry();
        savedEntry.setCondition("Mint");
        savedEntry.setPurchasePrice(new BigDecimal("29.99"));
        savedEntry.setQuantity(2);

        when(collectionEntryRepository.save(any(CollectionEntry.class))).thenReturn(savedEntry);

        CollectionEntryResponse response =
                collectionService.createEntry(testUser.getId(), request);

        assertThat(response.condition()).isEqualTo("Mint");
        assertThat(response.quantity()).isEqualTo(2);
    }

    @Test
    void createEntry_shouldThrowWhenUserNotFound() {
        UUID unknownUserId = UUID.randomUUID();
        CollectionEntryRequest request = new CollectionEntryRequest(
                testItem.getId(), null, null, null, null, null);

        when(userRepository.findById(unknownUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> collectionService.createEntry(unknownUserId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createEntry_shouldThrowWhenCatalogItemNotFound() {
        UUID unknownItemId = UUID.randomUUID();
        CollectionEntryRequest request = new CollectionEntryRequest(
                unknownItemId, null, null, null, null, null);

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(catalogItemRepository.findById(unknownItemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> collectionService.createEntry(testUser.getId(), request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateEntry_shouldModifyAndReturn() {
        CollectionEntry entry = createEntry();
        CollectionEntryRequest request = new CollectionEntryRequest(
                testItem.getId(), "Good", new BigDecimal("19.99"),
                LocalDate.of(2025, 6, 1), "Updated notes", 3);

        when(collectionEntryRepository.findByIdAndUserId(entry.getId(), testUser.getId()))
                .thenReturn(Optional.of(entry));
        when(collectionEntryRepository.save(any(CollectionEntry.class))).thenReturn(entry);

        CollectionEntryResponse response =
                collectionService.updateEntry(entry.getId(), testUser.getId(), request);

        assertThat(response).isNotNull();
    }

    @Test
    void deleteEntry_shouldRemoveEntry() {
        CollectionEntry entry = createEntry();
        when(collectionEntryRepository.findByIdAndUserId(entry.getId(), testUser.getId()))
                .thenReturn(Optional.of(entry));

        collectionService.deleteEntry(entry.getId(), testUser.getId());

        verify(collectionEntryRepository).delete(entry);
    }

    @Test
    void deleteEntry_shouldThrowWhenNotFound() {
        UUID entryId = UUID.randomUUID();
        when(collectionEntryRepository.findByIdAndUserId(entryId, testUser.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> collectionService.deleteEntry(entryId, testUser.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getUserCollectionPaged_firstPage_shouldReturnPageWithCursor() {
        List<CollectionEntry> entries = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            CollectionEntry entry = new CollectionEntry(testUser, testItem);
            entry.setId(UUID.randomUUID());
            entry.setCreatedAt(Instant.now().plusSeconds(i));
            entry.setQuantity(1);
            entries.add(entry);
        }
        when(collectionEntryRepository.findByUserIdFirstPage(eq(testUser.getId()),
                any(PageRequest.class))).thenReturn(entries);

        CursorPage<CollectionEntryResponse> page =
                collectionService.getUserCollectionPaged(testUser.getId(), null, 2);

        assertThat(page.content()).hasSize(2);
        assertThat(page.hasMore()).isTrue();
        assertThat(page.nextCursor()).isNotNull();
    }

    @Test
    void getUserCollectionPaged_withCursor_shouldQueryAfterCursor() {
        Instant ts = Instant.parse("2025-06-01T00:00:00Z");
        UUID cursorId = UUID.randomUUID();
        String cursor = CursorPaginationHelper.encode(ts, cursorId);

        CollectionEntry entry = createEntry();
        entry.setCreatedAt(Instant.now());
        when(collectionEntryRepository.findByUserIdAfterCursor(
                eq(testUser.getId()), eq(ts), eq(cursorId), any(PageRequest.class)))
                .thenReturn(List.of(entry));

        CursorPage<CollectionEntryResponse> page =
                collectionService.getUserCollectionPaged(testUser.getId(), cursor, 10);

        assertThat(page.content()).hasSize(1);
        assertThat(page.hasMore()).isFalse();
        assertThat(page.nextCursor()).isNull();
    }

    @Test
    void getUserCollectionPaged_emptyResult_shouldReturnEmptyPage() {
        when(collectionEntryRepository.findByUserIdFirstPage(eq(testUser.getId()),
                any(PageRequest.class))).thenReturn(List.of());

        CursorPage<CollectionEntryResponse> page =
                collectionService.getUserCollectionPaged(testUser.getId(), null, 10);

        assertThat(page.content()).isEmpty();
        assertThat(page.hasMore()).isFalse();
        assertThat(page.nextCursor()).isNull();
        assertThat(page.size()).isZero();
    }

    private CollectionEntry createEntry() {
        CollectionEntry entry = new CollectionEntry(testUser, testItem);
        entry.setId(UUID.randomUUID());
        entry.setQuantity(1);
        return entry;
    }
}
