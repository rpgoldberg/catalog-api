package com.catalogcollector.service;

import com.catalogcollector.controller.GlobalExceptionHandler.ResourceNotFoundException;
import com.catalogcollector.dto.CatalogItemRequest;
import com.catalogcollector.dto.CatalogItemResponse;
import com.catalogcollector.entity.CatalogItem;
import com.catalogcollector.repository.CatalogItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private CatalogItemRepository catalogItemRepository;

    private CatalogService catalogService;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(catalogItemRepository);
    }

    @Test
    void getAllItems_shouldReturnAllItems() {
        CatalogItem item = createTestItem();
        when(catalogItemRepository.findAll()).thenReturn(List.of(item));

        List<CatalogItemResponse> results = catalogService.getAllItems();

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().barcode()).isEqualTo("1234567890");
    }

    @Test
    void getItemById_shouldReturnItem() {
        CatalogItem item = createTestItem();
        when(catalogItemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        CatalogItemResponse response = catalogService.getItemById(item.getId());

        assertThat(response.name()).isEqualTo("Test Figure");
    }

    @Test
    void getItemById_shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(catalogItemRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.getItemById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void lookupByBarcode_shouldReturnItemForKnownBarcode() {
        CatalogItem item = createTestItem();
        when(catalogItemRepository.findByBarcode("1234567890")).thenReturn(Optional.of(item));

        CatalogItemResponse response = catalogService.lookupByBarcode("1234567890");

        assertThat(response.barcode()).isEqualTo("1234567890");
    }

    @Test
    void lookupByBarcode_shouldThrowForUnknownBarcode() {
        when(catalogItemRepository.findByBarcode("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.lookupByBarcode("unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createItem_shouldPersistAndReturn() {
        CatalogItemRequest request = new CatalogItemRequest(
                "9876543210", "EAN13", "New Item", "Brand", "Figures",
                "A description", "https://img.example.com/img.jpg", Map.of("key", "value"));

        CatalogItem saved = new CatalogItem("9876543210", "EAN13", "New Item");
        saved.setId(UUID.randomUUID());
        saved.setBrand("Brand");
        saved.setCategory("Figures");

        when(catalogItemRepository.save(any(CatalogItem.class))).thenReturn(saved);

        CatalogItemResponse response = catalogService.createItem(request);

        assertThat(response.barcode()).isEqualTo("9876543210");
        assertThat(response.name()).isEqualTo("New Item");
    }

    private CatalogItem createTestItem() {
        CatalogItem item = new CatalogItem("1234567890", "UPC", "Test Figure");
        item.setId(UUID.randomUUID());
        item.setBrand("TestBrand");
        item.setCategory("Figures");
        return item;
    }
}
