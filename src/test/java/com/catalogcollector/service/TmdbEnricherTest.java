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
class TmdbEnricherTest {

    private TmdbEnricher createEnricher(String baseUrl, String apiKey) {
        var props = new ProviderProperties(
                new ProviderProperties.OpenLibrary("http://unused"),
                new ProviderProperties.GoogleBooks("http://unused", ""),
                new ProviderProperties.UpcItemDb("http://unused"),
                null, null, null,
                new ProviderProperties.Tmdb(baseUrl, apiKey),
                null, null
        );
        return new TmdbEnricher(props, RestClient.create());
    }

    @Test
    void validTitleReturnsResult(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(get(urlPathEqualTo("/3/search/movie"))
                .withQueryParam("query", equalTo("The Dark Knight"))
                .withHeader("Authorization", equalTo("Bearer test-key"))
                .willReturn(okJson("""
                        {
                          "results": [{
                            "title": "The Dark Knight",
                            "poster_path": "/qJ2tW6WMUDux911Z6SbYF0OR0eE.jpg",
                            "release_date": "2008-07-16",
                            "overview": "Batman raises the stakes in his war on crime.",
                            "vote_average": 8.5
                          }]
                        }
                        """)));

        TmdbEnricher enricher = createEnricher(wmRuntimeInfo.getHttpBaseUrl(), "test-key");
        Optional<CatalogLookupResult> result = enricher.enrichByTitle("The Dark Knight", MediaType.BLU_RAY);

        assertTrue(result.isPresent());
        CatalogLookupResult r = result.get();
        assertEquals("The Dark Knight", r.title());
        assertEquals("https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911Z6SbYF0OR0eE.jpg", r.coverImageUrl());
        assertEquals("2008-07-16", r.releaseDate());
        assertNotNull(r.metadata());
        assertEquals("Batman raises the stakes in his war on crime.", r.metadata().get("synopsis"));
        assertEquals(8.5, r.metadata().get("rating"));
    }

    @Test
    void emptyResultsReturnsEmpty(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(get(urlPathEqualTo("/3/search/movie"))
                .willReturn(okJson("""
                        {"results": []}
                        """)));

        TmdbEnricher enricher = createEnricher(wmRuntimeInfo.getHttpBaseUrl(), "test-key");
        Optional<CatalogLookupResult> result = enricher.enrichByTitle("nonexistent", MediaType.BLU_RAY);

        assertTrue(result.isEmpty());
    }

    @Test
    void blankApiKeyReturnsEmpty() {
        TmdbEnricher enricher = createEnricher("http://unused", "");

        Optional<CatalogLookupResult> result = enricher.enrichByTitle("The Dark Knight", MediaType.BLU_RAY);

        assertTrue(result.isEmpty());
    }

    @Test
    void httpErrorReturnsEmpty(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(get(urlPathEqualTo("/3/search/movie"))
                .willReturn(serverError()));

        TmdbEnricher enricher = createEnricher(wmRuntimeInfo.getHttpBaseUrl(), "test-key");
        Optional<CatalogLookupResult> result = enricher.enrichByTitle("The Dark Knight", MediaType.BLU_RAY);

        assertTrue(result.isEmpty());
    }

    @Test
    void supportedMediaTypes() {
        TmdbEnricher enricher = createEnricher("http://unused", "test-key");

        assertEquals(Set.of(MediaType.BLU_RAY, MediaType.DVD, MediaType.VHS, MediaType.LASERDISC),
                enricher.getSupportedMediaTypes());
    }
}
