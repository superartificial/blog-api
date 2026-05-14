CREATE TABLE categories (
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR(200) NOT NULL,
    slug      VARCHAR(200) NOT NULL UNIQUE,
    parent_id BIGINT REFERENCES categories(id) ON DELETE SET NULL
);

ALTER TABLE posts ADD COLUMN category_id BIGINT REFERENCES categories(id) ON DELETE SET NULL;
