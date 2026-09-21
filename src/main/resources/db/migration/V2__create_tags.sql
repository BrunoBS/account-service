CREATE TABLE tags (
    id VARCHAR(36) NOT NULL,
    owner_type VARCHAR(50) NOT NULL,
    owner_id VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    origin_type VARCHAR(20) NOT NULL,
    CONSTRAINT pk_tags PRIMARY KEY (id),
    CONSTRAINT uk_tags_owner_name UNIQUE (owner_type, owner_id, name),
    CONSTRAINT ck_tags_origin_type CHECK (origin_type IN ('MANUAL', 'SYSTEM'))
);

CREATE INDEX idx_tags_owner
    ON tags (owner_type, owner_id);

CREATE INDEX idx_tags_lookup
    ON tags (owner_type, name);
