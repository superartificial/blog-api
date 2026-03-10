CREATE TABLE contact_submissions (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    email       VARCHAR(200)  NOT NULL,
    message     TEXT          NOT NULL,
    ip_address  VARCHAR(45),
    submitted_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    read        BOOLEAN       NOT NULL DEFAULT FALSE
);
