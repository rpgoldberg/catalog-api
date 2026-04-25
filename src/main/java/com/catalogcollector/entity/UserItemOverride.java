package com.catalogcollector.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_item_overrides",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_item_field",
                columnNames = {"user_id", "catalog_item_id", "field_name"}))
public class UserItemOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "catalog_item_id", nullable = false)
    private CatalogItem catalogItem;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(name = "override_value", nullable = false, columnDefinition = "TEXT")
    private String overrideValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OverridePolicy policy = OverridePolicy.AUTO_ACCEPT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UserItemOverride() {
    }

    public UserItemOverride(User user, CatalogItem catalogItem, String fieldName,
                            String overrideValue, OverridePolicy policy) {
        this.user = user;
        this.catalogItem = catalogItem;
        this.fieldName = fieldName;
        this.overrideValue = overrideValue;
        this.policy = policy;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public CatalogItem getCatalogItem() {
        return catalogItem;
    }

    public void setCatalogItem(CatalogItem catalogItem) {
        this.catalogItem = catalogItem;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getOverrideValue() {
        return overrideValue;
    }

    public void setOverrideValue(String overrideValue) {
        this.overrideValue = overrideValue;
    }

    public OverridePolicy getPolicy() {
        return policy;
    }

    public void setPolicy(OverridePolicy policy) {
        this.policy = policy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
