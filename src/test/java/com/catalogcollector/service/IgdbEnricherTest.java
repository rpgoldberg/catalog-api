package com.catalogcollector.service;

import com.catalogcollector.dto.CatalogLookupResult;
import com.catalogcollector.entity.MediaType;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@WireMockTest
@ExtendWith(MockitoExtension.class)
class IgdbEnricherTest {

    @Mock
    private OAuthTokenManager tokenManager;

    private IgdbEnricher enricher;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        enricher = new IgdbEnricher(wmRuntimeInfo.getHttpBaseUrl(), "test-client-id", RestClient.create(), tokenManager);
    }

    @Test
    void validTitleReturnsResult() {
        when(tokenManager.hasCredentials()).thenReturn(true);
        when(tokenManager.getAccessToken()).thenReturn("test-token");

        stubFor(post(urlPathEqualTo("/v4/games"))
                .withHeader("Client-ID", equalTo("test-client-id"))
                .withHeader("Authorization", equalTo("Bearer test-token"))
                .willReturn(okJson("""
                        [{
                          "name": "The Legend of Zelda: Breath of the Wild",
                          "cover": {"image_id": "co3p2d"},
                          "summary": "An open-world adventure game.",
                          "first_release_date": 1488499200
                        }]
                        """)));

        Optional<CatalogLookupResult> result = enricher.enrichByTitle("The Legend of Zelda: Breath of the Wild", MediaType.GAME);

        assertTrue(result.isPresent());
        CatalogLookupResult r = result.get();
        assertEquals("The Legend of Zelda: Breath of the Wild", r.title());
        assertEquals("https://images.igdb.com/igdb/image/upload/t_cover_big/co3p2d.jpg", r.coverImageUrl());
        assertEquals("2017-03-03", r.releaseDate());
        assertNotNull(r.metadata());
        assertEquals("An open-world adventure game.", r.metadata().get("synopsis"));
    }

    @Test
    void emptyResultsReturnsEmpty() {
        when(tokenManager.hasCredentials()).thenReturn(true);
        when(tokenManager.getAccessToken()).thenReturn("test-token");

        stubFor(post(urlPathEqualTo("/v4/games"))
                .willReturn(okJson("[]")));

        Optional<CatalogLookupResult> result = enricher.enrichByTitle("nonexistent", MediaType.GAME);

        assertTrue(result.isEmpty());
    }

    @Test
    void blankCredentialsReturnsEmpty() {
        when(tokenManager.hasCredentials()).thenReturn(false);

        Optional<CatalogLookupResult> result = enricher.enrichByTitle("The Legend of Zelda", MediaType.GAME);

        assertTrue(result.isEmpty());
    }

    @Test
    void tokenFailureReturnsEmpty() {
        when(tokenManager.hasCredentials()).thenReturn(true);
        when(tokenManager.getAccessToken()).thenReturn(null);

        Optional<CatalogLookupResult> result = enricher.enrichByTitle("The Legend of Zelda", MediaType.GAME);

        assertTrue(result.isEmpty());
    }

    @Test
    void httpErrorReturnsEmpty() {
        when(tokenManager.hasCredentials()).thenReturn(true);
        when(tokenManager.getAccessToken()).thenReturn("test-token");

        stubFor(post(urlPathEqualTo("/v4/games"))
                .willReturn(serverError()));

        Optional<CatalogLookupResult> result = enricher.enrichByTitle("The Legend of Zelda", MediaType.GAME);

        assertTrue(result.isEmpty());
    }

    @Test
    void escapesQuotesInTitle() {
        when(tokenManager.hasCredentials()).thenReturn(true);
        when(tokenManager.getAccessToken()).thenReturn("test-token");

        stubFor(post(urlPathEqualTo("/v4/games"))
                .willReturn(okJson("[]")));

        enricher.enrichByTitle("Assassin's Creed: \"Brotherhood\"", MediaType.GAME);

        verify(postRequestedFor(urlPathEqualTo("/v4/games"))
                .withRequestBody(containing("\\\"")));
    }

    @Test
    void supportedMediaTypes() {
        assertEquals(Set.of(MediaType.GAME), enricher.getSupportedMediaTypes());
    }
}
