package com.catalogcollector.service;

import com.catalogcollector.dto.CatalogLookupResult;
import com.catalogcollector.entity.MediaType;

import java.util.Optional;
import java.util.Set;

public interface SecondaryEnricher {

    Optional<CatalogLookupResult> enrichByTitle(String title, MediaType mediaType);

    String getSourceName();

    Set<MediaType> getSupportedMediaTypes();
}
