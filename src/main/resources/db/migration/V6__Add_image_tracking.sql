CREATE TABLE images (
    id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(255) NOT NULL UNIQUE,
    url VARCHAR(512) NOT NULL,
    mime_type VARCHAR(100),
    size_bytes BIGINT,
    uploaded_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE image_references (
    id BIGSERIAL PRIMARY KEY,
    image_id BIGINT NOT NULL REFERENCES images(id) ON DELETE CASCADE,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    UNIQUE (image_id, entity_type, entity_id, field_name)
);

CREATE INDEX idx_image_refs_entity ON image_references(entity_type, entity_id);
CREATE INDEX idx_image_refs_image ON image_references(image_id);
