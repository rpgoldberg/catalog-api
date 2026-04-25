package com.catalogcollector.service;

import com.catalogcollector.config.ProviderProperties;
import com.catalogcollector.dto.CatalogLookupResult;
import com.catalogcollector.entity.MediaType;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@WireMockTest
class AniListEnricherTest {

    private AniListEnricher createEnricher(String baseUrl) {
        var props = new ProviderProperties(
                new ProviderProperties.OpenLibrary("http://unused"),
                new ProviderProperties.GoogleBooks("http://unused", ""),
                new ProviderProperties.UpcItemDb("http://unused"),
                null, null, null, null, null,
                new ProviderProperties.AniList(baseUrl)
        );
        return new AniListEnricher(props,
                RestClient.builder().requestFactory(new SimpleClientHttpRequestFactory()).build());
    }

    @Test
    void validTitleReturnsResult(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(post(urlPathEqualTo("/")).willReturn(okJson("""
                {
                  "data": {
                    "Media": {
                      "title": {"english": "Attack on Titan", "romaji": "Shingeki no Kyojin"},
                      "coverImage": {"large": "https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/attack.jpg"},
                      "description": "<p>An <b>epic</b> adventure about titans.</p>",
                      "genres": ["Action", "Drama", "Fantasy"],
                      "averageScore": 84,
                      "volumes": 34,
                      "chapters": 141,
                      "startDate": {"year": 2009, "month": 9, "day": 9}
                    }
                  }
                }
                """)));

        AniListEnricher enricher = createEnricher(wmRuntimeInfo.getHttpBaseUrl());
        Optional<CatalogLookupResult> result = enricher.enrichByTitle("Attack on Titan", MediaType.MANGA);

        assertTrue(result.isPresent());
        CatalogLookupResult r = result.get();
        assertEquals("Attack on Titan", r.title());
        assertEquals("https://s4.anilist.co/file/anilistcdn/media/manga/cover/large/attack.jpg", r.coverImageUrl());
        assertEquals("2009-09-09", r.releaseDate());
        assertNotNull(r.metadata());
        assertEquals("An epic adventure about titans.", r.metadata().get("synopsis"));
        assertEquals(List.of("Action", "Drama", "Fantasy"), r.metadata().get("genres"));
        assertEquals(84, r.metadata().get("rating"));
        assertEquals(34, r.metadata().get("volumes"));
        assertEquals(141, r.metadata().get("chapters"));
    }

    @Test
    void prefersEnglishOverRomaji(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(post(urlPathEqualTo("/")).willReturn(okJson("""
                {
                  "data": {
                    "Media": {
                      "title": {"english": "Attack on Titan", "romaji": "Shingeki no Kyojin"},
                      "coverImage": null,
                      "description": null,
                      "genres": null,
                      "averageScore": null,
                      "volumes": null,
                      "chapters": null,
                      "startDate": null
                    }
                  }
                }
                """)));

        AniListEnricher enricher = createEnricher(wmRuntimeInfo.getHttpBaseUrl());
        Optional<CatalogLookupResult> result = enricher.enrichByTitle("Attack on Titan", MediaType.MANGA);

        assertTrue(result.isPresent());
        assertEquals("Attack on Titan", result.get().title());
    }

    @Test
    void fallsBackToRomaji(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(post(urlPathEqualTo("/")).willReturn(okJson("""
                {
                  "data": {
                    "Media": {
                      "title": {"english": null, "romaji": "Shingeki no Kyojin"},
                      "coverImage": null,
                      "description": null,
                      "genres": null,
                      "averageScore": null,
                      "volumes": null,
                      "chapters": null,
                      "startDate": null
                    }
                  }
                }
                """)));

        AniListEnricher enricher = createEnricher(wmRuntimeInfo.getHttpBaseUrl());
        Optional<CatalogLookupResult> result = enricher.enrichByTitle("Shingeki no Kyojin", MediaType.MANGA);

        assertTrue(result.isPresent());
        assertEquals("Shingeki no Kyojin", result.get().title());
    }

    @Test
    void emptyMediaReturnsEmpty(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(post(urlPathEqualTo("/")).willReturn(okJson("""
                {"data": {"Media": null}}
                """)));

        AniListEnricher enricher = createEnricher(wmRuntimeInfo.getHttpBaseUrl());
        Optional<CatalogLookupResult> result = enricher.enrichByTitle("nonexistent", MediaType.MANGA);

        assertTrue(result.isEmpty());
    }

    @Test
    void httpErrorReturnsEmpty(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(post(urlPathEqualTo("/")).willReturn(serverError()));

        AniListEnricher enricher = createEnricher(wmRuntimeInfo.getHttpBaseUrl());
        Optional<CatalogLookupResult> result = enricher.enrichByTitle("Attack on Titan", MediaType.MANGA);

        assertTrue(result.isEmpty());
    }

    @Test
    void stripsHtmlFromDescription(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(post(urlPathEqualTo("/")).willReturn(okJson("""
                {
                  "data": {
                    "Media": {
                      "title": {"english": "Test Manga", "romaji": "Test"},
                      "coverImage": null,
                      "description": "<p>An <b>epic</b> adventure.</p>",
                      "genres": null,
                      "averageScore": null,
                      "volumes": null,
                      "chapters": null,
                      "startDate": null
                    }
                  }
                }
                """)));

        AniListEnricher enricher = createEnricher(wmRuntimeInfo.getHttpBaseUrl());
        Optional<CatalogLookupResult> result = enricher.enrichByTitle("Test Manga", MediaType.MANGA);

        assertTrue(result.isPresent());
        assertEquals("An epic adventure.", result.get().metadata().get("synopsis"));
    }

    @Test
    void supportedMediaTypes() {
        AniListEnricher enricher = createEnricher("http://unused");

        assertEquals(Set.of(MediaType.MANGA, MediaType.LIGHT_NOVEL), enricher.getSupportedMediaTypes());
    }
}
