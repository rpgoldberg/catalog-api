package com.catalogcollector.dto;

import java.util.Map;

public record CatalogLookupResult(
        String title,
        String publisher,
        Integer pageCount,
        String coverImageUrl,
        String edition,
        String language,
        String releaseDate,
        Map<String, Object> metadata
) {
}
