package com.catalogcollector.service;

import com.catalogcollector.config.ProviderProperties;
import com.catalogcollector.dto.CatalogLookupResult;
import com.catalogcollector.entity.MediaType;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@WireMockTest
class GoogleBooksProviderTest {

    private GoogleBooksProvider createProvider(String baseUrl, String apiKey) {
        var props = new ProviderProperties(
                new ProviderProperties.OpenLibrary("http://unused"),
                new ProviderProperties.GoogleBooks(baseUrl, apiKey),
                new ProviderProperties.UpcItemDb("http://unused"),
                null, null, null, null, null, null
        );
        return new GoogleBooksProvider(props, RestClient.create());
    }

    @Test
    void validIsbnReturnsResult(WireMockRuntimeInfo wm) {
        stubFor(get(urlPathEqualTo("/books/v1/volumes"))
                .withQueryParam("q", equalTo("isbn:9784088820005"))
                .withQueryParam("key", equalTo("test-key"))
                .willReturn(okJson("""
                        {
                          "totalItems": 1,
                          "items": [{
                            "volumeInfo": {
                              "title": "One Piece Vol. 1",
                              "publisher": "Shueisha",
                              "pageCount": 208,
                              "imageLinks": {"thumbnail": "http://books.google.com/thumb.jpg"},
                              "language": "ja",
                              "publishedDate": "1997-12-24"
                            }
                          }]
                        }
                        """)));

        GoogleBooksProvider provider = createProvider(wm.getHttpBaseUrl(), "test-key");
        Optional<CatalogLookupResult> result = provider.lookupByBarcode("9784088820005", "EAN_13");

        assertTrue(result.isPresent());
        CatalogLookupResult r = result.get();
        assertEquals("One Piece Vol. 1", r.title());
        assertEquals("Shueisha", r.publisher());
        assertEquals(208, r.pageCount());
        assertEquals("http://books.google.com/thumb.jpg", r.coverImageUrl());
        assertEquals("ja", r.language());
        assertEquals("1997-12-24", r.releaseDate());
    }

    @Test
    void totalItemsZeroReturnsEmpty(WireMockRuntimeInfo wm) {
        stubFor(get(urlPathEqualTo("/books/v1/volumes"))
                .willReturn(okJson("""
                        {"totalItems": 0}
                        """)));

        GoogleBooksProvider provider = createProvider(wm.getHttpBaseUrl(), "test-key");
        Optional<CatalogLookupResult> result = provider.lookupByBarcode("0000000000000", "EAN_13");

        assertTrue(result.isEmpty());
    }

    @Test
    void blankApiKeyReturnsEmpty() {
        GoogleBooksProvider provider = createProvider("http://unused", "");
        Optional<CatalogLookupResult> result = provider.lookupByBarcode("9784088820005", "EAN_13");

        assertTrue(result.isEmpty());
    }

    @Test
    void nullApiKeyReturnsEmpty() {
        // ProviderProperties constructor will default null to ""
        GoogleBooksProvider provider = createProvider("http://unused", null);
        Optional<CatalogLookupResult> result = provider.lookupByBarcode("9784088820005", "EAN_13");

        assertTrue(result.isEmpty());
    }

    @Test
    void httpErrorReturnsEmpty(WireMockRuntimeInfo wm) {
        stubFor(get(urlPathEqualTo("/books/v1/volumes"))
                .willReturn(serverError()));

        GoogleBooksProvider provider = createProvider(wm.getHttpBaseUrl(), "test-key");
        Optional<CatalogLookupResult> result = provider.lookupByBarcode("9784088820005", "EAN_13");

        assertTrue(result.isEmpty());
    }

    @Test
    void supportedMediaTypes(WireMockRuntimeInfo wm) {
        GoogleBooksProvider provider = createProvider(wm.getHttpBaseUrl(), "test-key");
        assertEquals(Set.of(MediaType.MANGA, MediaType.LIGHT_NOVEL, MediaType.ART_BOOK, MediaType.MAGAZINE),
                provider.getSupportedMediaTypes());
    }

    @Test
    void sourceName(WireMockRuntimeInfo wm) {
        GoogleBooksProvider provider = createProvider(wm.getHttpBaseUrl(), "test-key");
        assertEquals("GoogleBooks", provider.getSourceName());
    }
}
