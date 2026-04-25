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
class MusicBrainzProviderTest {

    private MusicBrainzProvider provider;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        var props = new ProviderProperties(
                new ProviderProperties.OpenLibrary("http://unused"),
                new ProviderProperties.GoogleBooks("http://unused", ""),
                new ProviderProperties.UpcItemDb("http://unused"),
                null,
                new ProviderProperties.MusicBrainz(wmRuntimeInfo.getHttpBaseUrl(), "test@example.com"),
                null, null, null, null
        );
        provider = new MusicBrainzProvider(props, RestClient.create());
    }

    @Test
    void validBarcodeReturnsResult() {
        stubFor(get(urlPathEqualTo("/ws/2/release"))
                .withQueryParam("query", equalTo("barcode:0602537491070"))
                .willReturn(okJson("""
                        {
                          "releases": [{
                            "id": "abc-123-def",
                            "title": "Abbey Road",
                            "artist-credit": [{"name": "The Beatles"}],
                            "date": "1969-09-26",
                            "media": [{"format": "Vinyl"}]
                          }]
                        }
                        """)));

        Optional<CatalogLookupResult> result = provider.lookupByBarcode("0602537491070", "EAN_13");

        assertTrue(result.isPresent());
        CatalogLookupResult r = result.get();
        assertEquals("Abbey Road", r.title());
        assertEquals("The Beatles", r.publisher());
        assertEquals("1969-09-26", r.releaseDate());
        assertEquals("https://coverartarchive.org/release/abc-123-def/front-500", r.coverImageUrl());
        assertNotNull(r.metadata());
        assertEquals("Vinyl", r.metadata().get("format"));
    }

    @Test
    void emptyReleasesReturnsEmpty() {
        stubFor(get(urlPathEqualTo("/ws/2/release"))
                .willReturn(okJson("""
                        {"releases": []}
                        """)));

        Optional<CatalogLookupResult> result = provider.lookupByBarcode("0000000000000", "EAN_13");

        assertTrue(result.isEmpty());
    }

    @Test
    void httpErrorReturnsEmpty() {
        stubFor(get(urlPathEqualTo("/ws/2/release"))
                .willReturn(serverError()));

        Optional<CatalogLookupResult> result = provider.lookupByBarcode("0602537491070", "EAN_13");

        assertTrue(result.isEmpty());
    }

    @Test
    void userAgentHeaderSent() {
        stubFor(get(urlPathEqualTo("/ws/2/release"))
                .willReturn(okJson("""
                        {"releases": []}
                        """)));

        provider.lookupByBarcode("0602537491070", "EAN_13");

        verify(getRequestedFor(urlPathEqualTo("/ws/2/release"))
                .withHeader("User-Agent", containing("CatalogCollector")));
    }

    @Test
    void supportedMediaTypes() {
        assertEquals(Set.of(MediaType.OTHER), provider.getSupportedMediaTypes());
    }

    @Test
    void sourceName() {
        assertEquals("MusicBrainz", provider.getSourceName());
    }
}
