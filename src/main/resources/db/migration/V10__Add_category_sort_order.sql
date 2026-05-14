ALTER TABLE categories ADD COLUMN sort_order INT NOT NULL DEFAULT 0;

UPDATE categories c SET sort_order = sub.rn - 1
FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY parent_id ORDER BY name) - 1 AS rn
    FROM categories
) sub
WHERE c.id = sub.id;
