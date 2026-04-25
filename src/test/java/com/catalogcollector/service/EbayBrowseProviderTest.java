package com.catalogcollector.service;

import com.catalogcollector.dto.CatalogLookupResult;
import com.catalogcollector.entity.MediaType;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@WireMockTest
class EbayBrowseProviderTest {

    private OAuthTokenManager tokenManager;
    private EbayBrowseProvider provider;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        tokenManager = mock(OAuthTokenManager.class);
        provider = new EbayBrowseProvider(
                wmRuntimeInfo.getHttpBaseUrl(), RestClient.create(), tokenManager);
    }

    @Test
    void validGtinReturnsResult() {
        when(tokenManager.hasCredentials()).thenReturn(true);
        when(tokenManager.getAccessToken()).thenReturn("test-token");

        stubFor(get(urlPathEqualTo("/buy/browse/v1/item_summary/search"))
                .withQueryParam("gtin", equalTo("883929082063"))
                .willReturn(okJson("""
                        {
                          "itemSummaries": [{
                            "title": "The Dark Knight [Blu-ray]",
                            "image": {"imageUrl": "https://i.ebayimg.com/images/dk.jpg"},
                            "categories": [{"categoryId": "617", "categoryName": "Blu-ray Discs"}]
                          }]
                        }
                        """)));

        Optional<CatalogLookupResult> result = provider.lookupByBarcode("883929082063", "UPC_A");

        assertTrue(result.isPresent());
        CatalogLookupResult r = result.get();
        assertEquals("The Dark Knight [Blu-ray]", r.title());
        assertEquals("https://i.ebayimg.com/images/dk.jpg", r.coverImageUrl());
        assertNotNull(r.metadata());
        assertEquals("Blu-ray Discs", r.metadata().get("category"));

        verify(getRequestedFor(urlPathEqualTo("/buy/browse/v1/item_summary/search"))
                .withHeader("Authorization", equalTo("Bearer test-token")));
    }

    @Test
    void emptyResultsReturnsEmpty() {
        when(tokenManager.hasCredentials()).thenReturn(true);
        when(tokenManager.getAccessToken()).thenReturn("test-token");

        stubFor(get(urlPathEqualTo("/buy/browse/v1/item_summary/search"))
                .willReturn(okJson("""
                        {"itemSummaries": []}
                        """)));

        Optional<CatalogLookupResult> result = provider.lookupByBarcode("000000000000", "UPC_A");

        assertTrue(result.isEmpty());
    }

    @Test
    void blankCredentialsReturnsEmpty() {
        when(tokenManager.hasCredentials()).thenReturn(false);

        Optional<CatalogLookupResult> result = provider.lookupByBarcode("883929082063", "UPC_A");

        assertTrue(result.isEmpty());
    }

    @Test
    void tokenFailureReturnsEmpty() {
        when(tokenManager.hasCredentials()).thenReturn(true);
        when(tokenManager.getAccessToken()).thenReturn(null);

        Optional<CatalogLookupResult> result = provider.lookupByBarcode("883929082063", "UPC_A");

        assertTrue(result.isEmpty());
    }

    @Test
    void httpErrorReturnsEmpty() {
        when(tokenManager.hasCredentials()).thenReturn(true);
        when(tokenManager.getAccessToken()).thenReturn("test-token");

        stubFor(get(urlPathEqualTo("/buy/browse/v1/item_summary/search"))
                .willReturn(serverError()));

        Optional<CatalogLookupResult> result = provider.lookupByBarcode("883929082063", "UPC_A");

        assertTrue(result.isEmpty());
    }

    @Test
    void supportedMediaTypes() {
        assertEquals(Set.of(MediaType.values()), provider.getSupportedMediaTypes());
    }

    @Test
    void sourceName() {
        assertEquals("eBay", provider.getSourceName());
    }
}
