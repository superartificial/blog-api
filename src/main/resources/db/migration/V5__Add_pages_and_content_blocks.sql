CREATE TABLE pages (
    id               BIGSERIAL PRIMARY KEY,
    slug             VARCHAR(255) UNIQUE NOT NULL,
    title            VARCHAR(255) NOT NULL,
    meta_description TEXT,
    og_image_url     VARCHAR(512),
    status           VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE content_blocks (
    id           BIGSERIAL PRIMARY KEY,
    page_id      BIGINT NOT NULL REFERENCES pages(id) ON DELETE CASCADE,
    block_type   VARCHAR(50) NOT NULL,
    sort_order   INTEGER NOT NULL DEFAULT 0,
    content      JSONB NOT NULL DEFAULT '{}',
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_content_blocks_page ON content_blocks(page_id, sort_order);
