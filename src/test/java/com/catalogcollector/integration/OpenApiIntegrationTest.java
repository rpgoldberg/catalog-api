package com.catalogcollector.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void apiDocsEndpoint_shouldBePublicAndReturnOpenApiSpec() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/v3/api-docs", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"openapi\"");
        assertThat(response.getBody()).contains("Catalog API");
    }

    @Test
    void swaggerUiEndpoint_shouldBeAccessible() {
        ResponseEntity<String> response =
                restTemplate.getForEntity("/swagger-ui.html", String.class);

        // Swagger UI redirects to the actual UI page
        assertThat(response.getStatusCode().value()).isIn(200, 302);
    }
}
