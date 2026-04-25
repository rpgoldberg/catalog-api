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
class BestBuyProviderTest {

    private BestBuyProvider provider;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        var props = new ProviderProperties(
                new ProviderProperties.OpenLibrary("http://unused"),
                new ProviderProperties.GoogleBooks("http://unused", ""),
                new ProviderProperties.UpcItemDb("http://unused"),
                new ProviderProperties.BestBuy(wmRuntimeInfo.getHttpBaseUrl(), "test-key"),
                null, null, null, null, null
        );
        provider = new BestBuyProvider(props, RestClient.create());
    }

    @Test
    void validUpcReturnsResult() {
        stubFor(get(urlPathEqualTo("/v1/products(upc=883929082063)"))
                .withQueryParam("apiKey", equalTo("test-key"))
                .withQueryParam("format", equalTo("json"))
                .willReturn(okJson("""
                        {
                          "products": [{
                            "name": "The Dark Knight [Blu-ray]",
                            "upc": "883929082063",
                            "largeImage": "https://pisces.bbystatic.com/large.jpg",
                            "image": "https://pisces.bbystatic.com/small.jpg",
                            "manufacturer": "Warner Home Video",
                            "releaseDate": "2008-12-09",
                            "longDescription": "Batman faces the Joker.",
                            "categoryPath": [
                              {"name": "Movies & Music"},
                              {"name": "Movies"},
                              {"name": "Blu-ray"}
                            ]
                          }]
                        }
                        """)));

        Optional<CatalogLookupResult> result = provider.lookupByBarcode("883929082063", "UPC_A");

        assertTrue(result.isPresent());
        CatalogLookupResult r = result.get();
        assertEquals("The Dark Knight [Blu-ray]", r.title());
        assertEquals("Warner Home Video", r.publisher());
        assertNull(r.pageCount());
        assertEquals("https://pisces.bbystatic.com/large.jpg", r.coverImageUrl());
        assertEquals("2008-12-09", r.releaseDate());
        assertNotNull(r.metadata());
        assertEquals("Batman faces the Joker.", r.metadata().get("description"));
        assertEquals("Blu-ray", r.metadata().get("category"));
    }

    @Test
    void emptyProductsReturnsEmpty() {
        stubFor(get(urlPathMatching("/v1/products.*"))
                .willReturn(okJson("""
                        {"products": []}
                        """)));

        Optional<CatalogLookupResult> result = provider.lookupByBarcode("000000000000", "UPC_A");

        assertTrue(result.isEmpty());
    }

    @Test
    void blankApiKeyReturnsEmpty() {
        var props = new ProviderProperties(
                new ProviderProperties.OpenLibrary("http://unused"),
                new ProviderProperties.GoogleBooks("http://unused", ""),
                new ProviderProperties.UpcItemDb("http://unused"),
                new ProviderProperties.BestBuy("http://unused", ""),
                null, null, null, null, null
        );
        BestBuyProvider blankKeyProvider = new BestBuyProvider(props, RestClient.create());

        Optional<CatalogLookupResult> result = blankKeyProvider.lookupByBarcode("883929082063", "UPC_A");

        assertTrue(result.isEmpty());
    }

    @Test
    void httpErrorReturnsEmpty() {
        stubFor(get(urlPathMatching("/v1/products.*"))
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
        assertEquals("BestBuy", provider.getSourceName());
    }

    @Test
    void fallsBackToImageWhenNoLargeImage() {
        stubFor(get(urlPathMatching("/v1/products.*"))
                .willReturn(okJson("""
                        {
                          "products": [{
                            "name": "Some Movie",
                            "image": "https://pisces.bbystatic.com/small.jpg",
                            "manufacturer": "Studio"
                          }]
                        }
                        """)));

        Optional<CatalogLookupResult> result = provider.lookupByBarcode("883929082063", "UPC_A");

        assertTrue(result.isPresent());
        assertEquals("https://pisces.bbystatic.com/small.jpg", result.get().coverImageUrl());
    }
}
