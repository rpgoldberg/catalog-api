package com.catalogcollector.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "catalog_items")
public class CatalogItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String barcode;

    @Column(name = "barcode_type", nullable = false)
    private String barcodeType;

    // Canonical fields (from external lookup sources)

    @Column(name = "canonical_title")
    private String canonicalTitle;

    @Column(name = "canonical_publisher")
    private String canonicalPublisher;

    @Column(name = "canonical_page_count")
    private Integer canonicalPageCount;

    @Column(name = "canonical_cover_image_url")
    private String canonicalCoverImageUrl;

    @Column(name = "canonical_edition")
    private String canonicalEdition;

    @Column(name = "canonical_language")
    private String canonicalLanguage;

    @Column(name = "canonical_release_date")
    private String canonicalReleaseDate;

    // Source tracking

    @Column(name = "lookup_source")
    private String lookupSource;

    @Column(name = "last_enriched_at")
    private Instant lastEnrichedAt;

    // Media type for category-specific metadata

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type")
    private MediaType mediaType;

    // JSONB for media-type-specific canonical metadata

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "canonical_metadata", columnDefinition = "jsonb")
    private Map<String, Object> canonicalMetadata;

    // Dedup + sync

    @Column(name = "client_id", unique = true)
    private String clientId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public CatalogItem() {
    }

    public CatalogItem(String barcode, String barcodeType, String canonicalTitle) {
        this.barcode = barcode;
        this.barcodeType = barcodeType;
        this.canonicalTitle = canonicalTitle;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getBarcodeType() {
        return barcodeType;
    }

    public void setBarcodeType(String barcodeType) {
        this.barcodeType = barcodeType;
    }

    public String getCanonicalTitle() {
        return canonicalTitle;
    }

    public void setCanonicalTitle(String canonicalTitle) {
        this.canonicalTitle = canonicalTitle;
    }

    public String getCanonicalPublisher() {
        return canonicalPublisher;
    }

    public void setCanonicalPublisher(String canonicalPublisher) {
        this.canonicalPublisher = canonicalPublisher;
    }

    public Integer getCanonicalPageCount() {
        return canonicalPageCount;
    }

    public void setCanonicalPageCount(Integer canonicalPageCount) {
        this.canonicalPageCount = canonicalPageCount;
    }

    public String getCanonicalCoverImageUrl() {
        return canonicalCoverImageUrl;
    }

    public void setCanonicalCoverImageUrl(String canonicalCoverImageUrl) {
        this.canonicalCoverImageUrl = canonicalCoverImageUrl;
    }

    public String getCanonicalEdition() {
        return canonicalEdition;
    }

    public void setCanonicalEdition(String canonicalEdition) {
        this.canonicalEdition = canonicalEdition;
    }

    public String getCanonicalLanguage() {
        return canonicalLanguage;
    }

    public void setCanonicalLanguage(String canonicalLanguage) {
        this.canonicalLanguage = canonicalLanguage;
    }

    public String getCanonicalReleaseDate() {
        return canonicalReleaseDate;
    }

    public void setCanonicalReleaseDate(String canonicalReleaseDate) {
        this.canonicalReleaseDate = canonicalReleaseDate;
    }

    public String getLookupSource() {
        return lookupSource;
    }

    public void setLookupSource(String lookupSource) {
        this.lookupSource = lookupSource;
    }

    public Instant getLastEnrichedAt() {
        return lastEnrichedAt;
    }

    public void setLastEnrichedAt(Instant lastEnrichedAt) {
        this.lastEnrichedAt = lastEnrichedAt;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public Map<String, Object> getCanonicalMetadata() {
        return canonicalMetadata;
    }

    public void setCanonicalMetadata(Map<String, Object> canonicalMetadata) {
        this.canonicalMetadata = canonicalMetadata;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
