-- Add soft-delete and client-id columns for offline-first sync support.

-- Users: soft delete
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMPTZ;

-- Catalog items: soft delete + offline client dedup
ALTER TABLE catalog_items ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE catalog_items ADD COLUMN client_id VARCHAR(36) UNIQUE;

-- Collection entries: soft delete + offline client dedup
ALTER TABLE collection_entries ADD COLUMN deleted_at TIMESTAMPTZ;
ALTER TABLE collection_entries ADD COLUMN client_id VARCHAR(36) UNIQUE;

-- Indexes on updated_at for delta sync queries
CREATE INDEX idx_catalog_items_updated_at ON catalog_items (updated_at);
CREATE INDEX idx_collection_entries_updated_at ON collection_entries (updated_at);
CREATE INDEX idx_users_updated_at ON users (updated_at);
