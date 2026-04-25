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
class OpenLibraryProviderTest {

    private OpenLibraryProvider provider;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        var props = new ProviderProperties(
                new ProviderProperties.OpenLibrary(wmRuntimeInfo.getHttpBaseUrl()),
                new ProviderProperties.GoogleBooks("http://unused", ""),
                new ProviderProperties.UpcItemDb("http://unused"),
                null, null, null, null, null, null
        );
        provider = new OpenLibraryProvider(props, RestClient.create());
    }

    @Test
    void validIsbnReturnsResult() {
        stubFor(get(urlPathEqualTo("/api/books"))
                .withQueryParam("bibkeys", equalTo("ISBN:9784088820005"))
                .withQueryParam("jscmd", equalTo("data"))
                .withQueryParam("format", equalTo("json"))
                .willReturn(okJson("""
                        {
                          "ISBN:9784088820005": {
                            "title": "One Piece Vol. 1",
                            "publishers": [{"name": "Shueisha"}],
                            "number_of_pages": 208,
                            "languages": [{"key": "/languages/jpn"}],
                            "publish_date": "1997"
                          }
                        }
                        """)));

        Optional<CatalogLookupResult> result = provider.lookupByBarcode("9784088820005", "EAN_13");

        assertTrue(result.isPresent());
        CatalogLookupResult r = result.get();
        assertEquals("One Piece Vol. 1", r.title());
        assertEquals("Shueisha", r.publisher());
        assertEquals(208, r.pageCount());
        assertEquals("https://covers.openlibrary.org/b/isbn/9784088820005-M.jpg", r.coverImageUrl());
        assertEquals("jpn", r.language());
        assertEquals("1997", r.releaseDate());
    }

    @Test
    void emptyResponseReturnsEmpty() {
        stubFor(get(urlPathEqualTo("/api/books"))
                .willReturn(okJson("{}")));

        Optional<CatalogLookupResult> result = provider.lookupByBarcode("0000000000000", "EAN_13");

        assertTrue(result.isEmpty());
    }

    @Test
    void httpErrorReturnsEmpty() {
        stubFor(get(urlPathEqualTo("/api/books"))
                .willReturn(serverError()));

        Optional<CatalogLookupResult> result = provider.lookupByBarcode("9784088820005", "EAN_13");

        assertTrue(result.isEmpty());
    }

    @Test
    void supportedMediaTypes() {
        assertEquals(Set.of(MediaType.MANGA, MediaType.LIGHT_NOVEL, MediaType.ART_BOOK, MediaType.MAGAZINE),
                provider.getSupportedMediaTypes());
    }

    @Test
    void sourceName() {
        assertEquals("OpenLibrary", provider.getSourceName());
    }
}
