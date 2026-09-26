INSERT IGNORE INTO type_life_cycle
    (code, label, description, sort_order, is_active, settings)
VALUES
    ('ACTIVE', 'Active', 'Active lifecycle state', 1, true, '{}'),
    ('INACTIVE', 'Inactive', 'Inactive lifecycle state', 2, true, '{}'),
    ('QUARANTINED', 'Quarantined', 'Quarantined lifecycle state', 3, true, '{}');

INSERT IGNORE INTO type_schema_scopes
    (code, label, description, sort_order, is_active, settings)
VALUES
    ('PLATFORM', 'Platform', 'Schema owned by the platform', 1, true, '{}'),
    ('WORKSPACE', 'Workspace', 'Schema owned by a workspace', 2, true, '{}');

CREATE TABLE schema_types (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    lifecycle_code VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_schema_types PRIMARY KEY (id),
    CONSTRAINT uk_schema_types_identifier UNIQUE (identifier),
    CONSTRAINT uk_schema_types_code UNIQUE (code),
    CONSTRAINT uk_schema_types_name UNIQUE (name),
    CONSTRAINT ck_schema_types_code CHECK (code REGEXP '^[a-z][a-z0-9]*(-[a-z0-9]+)*$'),
    CONSTRAINT fk_schema_types_lifecycle
        FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code)
);

CREATE TABLE schema_definitions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL,
    schema_type_id BIGINT NOT NULL,
    scope_code VARCHAR(50) NOT NULL,
    workspace_identifier VARCHAR(36),
    owner_key VARCHAR(36) GENERATED ALWAYS AS (COALESCE(workspace_identifier, 'PLATFORM')) STORED,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    lifecycle_code VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_schema_definitions PRIMARY KEY (id),
    CONSTRAINT uk_schema_definitions_identifier UNIQUE (identifier),
    CONSTRAINT uk_schema_definitions_scope_owner_type_code
        UNIQUE (scope_code, owner_key, schema_type_id, code),
    CONSTRAINT ck_schema_definitions_code CHECK (code REGEXP '^[a-z][a-z0-9]*(-[a-z0-9]+)*$'),
    CONSTRAINT ck_schema_definitions_owner
        CHECK (
            (scope_code = 'PLATFORM' AND workspace_identifier IS NULL)
            OR
            (scope_code = 'WORKSPACE' AND workspace_identifier IS NOT NULL)
        ),
    CONSTRAINT fk_schema_definitions_type
        FOREIGN KEY (schema_type_id) REFERENCES schema_types(id),
    CONSTRAINT fk_schema_definitions_scope
        FOREIGN KEY (scope_code) REFERENCES type_schema_scopes(code),
    CONSTRAINT fk_schema_definitions_lifecycle
        FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code)
);

CREATE TABLE schema_versions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    identifier VARCHAR(36) NOT NULL,
    schema_id BIGINT NOT NULL,
    schema_version INT NOT NULL,
    version_name VARCHAR(100) NOT NULL,
    definition JSON NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_schema_versions PRIMARY KEY (id),
    CONSTRAINT uk_schema_versions_identifier UNIQUE (identifier),
    CONSTRAINT uk_schema_versions_schema_version UNIQUE (schema_id, schema_version),
    CONSTRAINT ck_schema_versions_status CHECK (status IN ('DRAFT', 'PUBLISHED')),
    CONSTRAINT fk_schema_versions_schema
        FOREIGN KEY (schema_id) REFERENCES schema_definitions(id)
);

CREATE INDEX idx_schema_definitions_type_scope_owner
    ON schema_definitions (schema_type_id, scope_code, workspace_identifier);

CREATE INDEX idx_schema_versions_resolution
    ON schema_versions (schema_id, status, schema_version);

INSERT INTO schema_types
    (version, identifier, code, name, description, lifecycle_code, created_at, updated_at)
VALUES
    (0, UUID(), 'default', 'Default', 'Fallback schema type for dynamic settings', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO schema_definitions
    (version, identifier, schema_type_id, scope_code, workspace_identifier, code, name, description,
     lifecycle_code, created_at, updated_at)
SELECT
    0, UUID(), st.id, 'PLATFORM', NULL, 'default', 'Default',
    'Permissive fallback schema for platform settings',
    'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_types st
WHERE st.code = 'default';

INSERT INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT
    UUID(), s.id, 1, 'v1',
    '{"$schema":"https://json-schema.org/draft/2020-12/schema","title":"Default Dynamic Schema","type":"object","additionalProperties":true}',
    'PUBLISHED',
    CURRENT_TIMESTAMP
FROM schema_definitions s
JOIN schema_types st ON st.id = s.schema_type_id
WHERE st.code = 'default'
  AND s.scope_code = 'PLATFORM'
  AND s.code = 'default';
