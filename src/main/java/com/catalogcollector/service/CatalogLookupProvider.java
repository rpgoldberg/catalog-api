package com.catalogcollector.service;

import com.catalogcollector.dto.CatalogLookupResult;
import com.catalogcollector.entity.MediaType;

import java.util.Optional;
import java.util.Set;

public interface CatalogLookupProvider {

    Optional<CatalogLookupResult> lookupByBarcode(String barcode, String barcodeType);

    String getSourceName();

    Set<MediaType> getSupportedMediaTypes();
}
