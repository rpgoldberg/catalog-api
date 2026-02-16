-- Users table
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    display_name    VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    subscription_tier VARCHAR(20) NOT NULL DEFAULT 'FREE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email ON users (email);

-- Catalog items table
CREATE TABLE catalog_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    barcode         VARCHAR(128) NOT NULL,
    barcode_type    VARCHAR(50)  NOT NULL,
    name            VARCHAR(512) NOT NULL,
    brand           VARCHAR(255),
    category        VARCHAR(255),
    description     TEXT,
    image_url       VARCHAR(2048),
    metadata        JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_catalog_items_barcode ON catalog_items (barcode);

-- Collection entries table
CREATE TABLE collection_entries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    catalog_item_id UUID NOT NULL REFERENCES catalog_items(id) ON DELETE CASCADE,
    item_condition   VARCHAR(50),
    purchase_price  NUMERIC(10,2),
    purchase_date   DATE,
    notes           TEXT,
    quantity        INTEGER NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_collection_entries_user_id ON collection_entries (user_id);
CREATE INDEX idx_collection_entries_catalog_item_id ON collection_entries (catalog_item_id);

-- NOTE: PostgreSQL AGE extension for graph queries (franchise/series relationships)
-- will be added in a future migration once AGE is configured.
