package com.catalogcollector.controller;

import com.catalogcollector.controller.GlobalExceptionHandler.ResourceNotFoundException;
import com.catalogcollector.dto.ApiError;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private WebRequest request(String path) {
        MockHttpServletRequest mock = new MockHttpServletRequest("GET", path);
        return new ServletWebRequest(mock);
    }

    @Test
    void handleResourceNotFound_returnsConsistentApiError() {
        ResponseEntity<ApiError> response = handler.handleNotFound(
                new ResourceNotFoundException("Item not found"), request("/catalog/items/123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(404);
        assertThat(body.error()).isEqualTo("Not Found");
        assertThat(body.message()).isEqualTo("Item not found");
        assertThat(body.path()).isEqualTo("/catalog/items/123");
        assertThat(body.timestamp()).isNotNull();
        assertThat(body.fieldErrors()).isNull();
    }

    @Test
    void handleIllegalArgument_returnsBadRequestApiError() {
        ResponseEntity<ApiError> response = handler.handleIllegalArgument(
                new IllegalArgumentException("Email already registered"), request("/auth/register"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(400);
        assertThat(body.error()).isEqualTo("Bad Request");
        assertThat(body.message()).isEqualTo("Email already registered");
        assertThat(body.path()).isEqualTo("/auth/register");
    }

    @Test
    void handleGenericException_returnsInternalServerError() {
        ResponseEntity<ApiError> response = handler.handleGenericException(
                new RuntimeException("Something broke"), request("/collections"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(500);
        assertThat(body.error()).isEqualTo("Internal Server Error");
        assertThat(body.message()).isEqualTo("An unexpected error occurred");
        assertThat(body.path()).isEqualTo("/collections");
    }
}
