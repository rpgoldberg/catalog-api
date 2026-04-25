-- Expand catalog_items with canonical enrichment fields and media type.
-- Rename existing columns to canonical naming convention.

-- Rename name → canonical_title
ALTER TABLE catalog_items RENAME COLUMN name TO canonical_title;
ALTER TABLE catalog_items ALTER COLUMN canonical_title DROP NOT NULL;

-- Rename brand → canonical_publisher
ALTER TABLE catalog_items RENAME COLUMN brand TO canonical_publisher;

-- Rename image_url → canonical_cover_image_url
ALTER TABLE catalog_items RENAME COLUMN image_url TO canonical_cover_image_url;

-- Rename metadata → canonical_metadata
ALTER TABLE catalog_items RENAME COLUMN metadata TO canonical_metadata;

-- Rename category → media_type
ALTER TABLE catalog_items RENAME COLUMN category TO media_type;

-- Add new canonical fields
ALTER TABLE catalog_items ADD COLUMN canonical_page_count INTEGER;
ALTER TABLE catalog_items ADD COLUMN canonical_edition VARCHAR(255);
ALTER TABLE catalog_items ADD COLUMN canonical_language VARCHAR(50);
ALTER TABLE catalog_items ADD COLUMN canonical_release_date VARCHAR(50);

-- Source tracking fields
ALTER TABLE catalog_items ADD COLUMN lookup_source VARCHAR(50);
ALTER TABLE catalog_items ADD COLUMN last_enriched_at TIMESTAMPTZ;

-- Drop description column (data moves to canonical_metadata if needed)
ALTER TABLE catalog_items DROP COLUMN IF EXISTS description;

-- Create user_item_overrides table
CREATE TABLE user_item_overrides (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    catalog_item_id  UUID NOT NULL REFERENCES catalog_items(id) ON DELETE CASCADE,
    field_name       VARCHAR(100) NOT NULL,
    override_value   TEXT NOT NULL,
    policy           VARCHAR(20) NOT NULL DEFAULT 'AUTO_ACCEPT',
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_item_field UNIQUE (user_id, catalog_item_id, field_name)
);

CREATE INDEX idx_user_item_overrides_user_id ON user_item_overrides (user_id);
CREATE INDEX idx_user_item_overrides_catalog_item_id ON user_item_overrides (catalog_item_id);
CREATE INDEX idx_user_item_overrides_user_item ON user_item_overrides (user_id, catalog_item_id);
CREATE INDEX idx_user_item_overrides_updated_at ON user_item_overrides (updated_at);

-- Create pending_updates table
CREATE TABLE pending_updates (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    catalog_item_id  UUID NOT NULL REFERENCES catalog_items(id) ON DELETE CASCADE,
    field_name       VARCHAR(100) NOT NULL,
    old_value        TEXT,
    new_value        TEXT,
    source           VARCHAR(50),
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at      TIMESTAMPTZ,
    CONSTRAINT chk_pending_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED'))
);

CREATE INDEX idx_pending_updates_user_id ON pending_updates (user_id);
CREATE INDEX idx_pending_updates_user_status ON pending_updates (user_id, status);
CREATE INDEX idx_pending_updates_catalog_item_id ON pending_updates (catalog_item_id);
