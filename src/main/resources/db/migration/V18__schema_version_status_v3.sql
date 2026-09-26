CREATE TABLE type_schema_version_status (
    code VARCHAR(50) NOT NULL,
    label VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order INT NOT NULL,
    is_active BOOLEAN NOT NULL,
    settings TEXT NOT NULL,
    CONSTRAINT pk_type_schema_version_status PRIMARY KEY (code),
    CONSTRAINT ck_type_schema_version_status_code
        CHECK (code REGEXP '^[A-Z][A-Z0-9_]{0,49}$')
);

INSERT INTO type_schema_version_status
    (code, label, description, sort_order, is_active, settings)
VALUES
    ('DRAFT', 'Draft', 'Schema version under edition and not available for consumption', 1, true, '{}'),
    ('PUBLISHED', 'Published', 'Published immutable schema version available for consumption', 2, true, '{}');

ALTER TABLE schema_versions
    ADD COLUMN draft_schema_id BIGINT
        GENERATED ALWAYS AS (
            CASE WHEN status = 'DRAFT' THEN schema_id ELSE NULL END
        ) STORED,
    ADD CONSTRAINT uk_schema_versions_single_draft UNIQUE (draft_schema_id),
    ADD CONSTRAINT fk_schema_versions_status
        FOREIGN KEY (status) REFERENCES type_schema_version_status(code);
