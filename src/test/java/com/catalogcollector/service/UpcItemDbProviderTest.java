package com.catalogcollector.service;

import com.catalogcollector.config.ProviderProperties;
import com.catalogcollector.dto.CatalogLookupResult;
import com.catalogcollector.entity.MediaType;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@WireMockTest
class UpcItemDbProviderTest {

    private UpcItemDbProvider provider;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        var props = new ProviderProperties(
                new ProviderProperties.OpenLibrary("http://unused"),
                new ProviderProperties.GoogleBooks("http://unused", ""),
                new ProviderProperties.UpcItemDb(wmRuntimeInfo.getHttpBaseUrl()),
                null, null, null, null, null, null
        );
        provider = new UpcItemDbProvider(props, RestClient.create());
    }

    @Test
    void validUpcReturnsResult() {
        stubFor(get(urlPathEqualTo("/prod/trial/lookup"))
                .withQueryParam("upc", equalTo("883929082063"))
                .willReturn(okJson("""
                        {
                          "code": "OK",
                          "total": 1,
                          "items": [{
                            "title": "The Dark Knight [Blu-ray]",
                            "brand": "Warner Home Video",
                            "description": "Batman raises the stakes in his war on crime.",
                            "images": ["https://images.upcitemdb.com/dk.jpg"]
                          }]
                        }
                        """)));

        Optional<CatalogLookupResult> result = provider.lookupByBarcode("883929082063", "UPC_A");

        assertTrue(result.isPresent());
        CatalogLookupResult r = result.get();
        assertEquals("The Dark Knight [Blu-ray]", r.title());
        assertEquals("Warner Home Video", r.publisher());
        assertNull(r.pageCount());
        assertEquals("https://images.upcitemdb.com/dk.jpg", r.coverImageUrl());
        assertNotNull(r.metadata());
        assertEquals("Batman raises the stakes in his war on crime.", r.metadata().get("description"));
    }

    @Test
    void emptyItemsReturnsEmpty() {
        stubFor(get(urlPathEqualTo("/prod/trial/lookup"))
                .willReturn(okJson("""
                        {"code": "OK", "total": 0, "items": []}
                        """)));

        Optional<CatalogLookupResult> result = provider.lookupByBarcode("000000000000", "UPC_A");

        assertTrue(result.isEmpty());
    }

    @Test
    void httpErrorReturnsEmpty() {
        stubFor(get(urlPathEqualTo("/prod/trial/lookup"))
                .willReturn(serverError()));

        Optional<CatalogLookupResult> result = provider.lookupByBarcode("883929082063", "UPC_A");

        assertTrue(result.isEmpty());
    }

    @Test
    void supportedMediaTypes() {
        assertEquals(Set.of(MediaType.BLU_RAY, MediaType.DVD, MediaType.VHS,
                        MediaType.LASERDISC, MediaType.GAME, MediaType.OTHER),
                provider.getSupportedMediaTypes());
    }

    @Test
    void sourceName() {
        assertEquals("UPCitemdb", provider.getSourceName());
    }
}
