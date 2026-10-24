-- Minimal workspace HTTP fixture: generic fallback schema, lifecycle codes and the
-- workspace-specific schema contract. Loaded by the platform-testing database annotation.
-- ACTIVE is the lifecycle root required by the catalog API's lifecycle foreign key.
INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings)
VALUES ('ACTIVE', 'Active', 'Active lifecycle', 1, true, '{}');

INSERT IGNORE INTO type_schema_scopes (code, label, description, sort_order, is_active, settings)
VALUES ('PLATFORM', 'Platform', 'Platform schema scope', 1, true, '{}'),
       ('WORKSPACE', 'Workspace', 'Workspace schema scope', 2, true, '{}');

INSERT IGNORE INTO type_schema_version_status (code, label, description, sort_order, is_active, settings)
VALUES ('DRAFT', 'Draft', 'Draft schema version', 1, true, '{}'),
       ('PUBLISHED', 'Published', 'Published schema version', 2, true, '{}');

INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES (0, UUID(), 'PLATFORM', 'default', 'Default', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 1, 'Default', '{"type":"object","additionalProperties":true}',
       'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'default';

INSERT IGNORE INTO schema_binding
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'CATALOG', 'DEFAULT', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'default';

INSERT IGNORE INTO schema_binding
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'CATALOG', 'lifecycle-type', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'default';

INSERT IGNORE INTO schema_binding
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'CATALOG', 'workspace-type', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'default';

INSERT IGNORE INTO schema_binding
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'CATALOG', 'application-scope-type', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'default';

INSERT IGNORE INTO schema_binding
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'CATALOG', 'onboarding-phase-type', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'default';

-- One structural contract for Workspace creation and update.
INSERT IGNORE INTO
    schema_definitions (
        version,
        identifier,
        scope_code,
        code,
        name,
        lifecycle_code,
        created_at,
        updated_at
    )
VALUES
    (
        0,
        UUID(),
        'PLATFORM',
        'workspace-resource',
        'Workspace Resource',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );

INSERT IGNORE INTO
    schema_versions (
        identifier,
        schema_id,
        schema_version,
        version_name,
        definition,
        status,
        created_at
    )
SELECT
    UUID(),
    id,
    1,
    'Workspace Resource v1',
    '{
    "type": "object",
    "additionalProperties": false,
    "required": [
        "workspaceType",
        "name",
        "description",
        "requester",
        "acronym",
        "emailGroup",
        "approvers"
    ],
    "properties": {
        "workspaceType": {
            "type": "string",
            "minLength": 1,
            "enum": [
                "ADMIN",
                "MANAGER"
            ],
            "maxLength": 20
        },
        "name": {
            "type": "string",
            "minLength": 3,
            "maxLength": 80,
            "pattern": "\\\\S"
        },
        "description": {
            "type": "string",
            "minLength": 10,
            "maxLength": 200,
            "pattern": "\\\\S"
        },
        "requester": {
            "type": "string",
            "minLength": 5,
            "maxLength": 10,
            "pattern": "\\\\S"
        },
        "acronym": {
            "type": "string",
            "minLength": 1,
            "maxLength": 5,
            "pattern": "\\\\S"
        },
        "authorizerGroup": {
            "type": [
                "string",
                "null"
            ],
            "maxLength": 20
        },
        "settings": {
            "type": [
                "object",
                "null"
            ],
            "additionalProperties": true
        },
        "emailGroup": {
            "type": "string",
            "minLength": 1,
            "maxLength": 200,
            "pattern": "^[^\\\\s@]+@[^\\\\s@]+\\\\.[^\\\\s@]+$",
            "format": "email"
        },
        "approvers": {
            "type": "array",
            "minItems": 1,
            "items": {
                "type": "object",
                "additionalProperties": false,
                "required": [
                    "functional",
                    "email"
                ],
                "properties": {
                    "functional": {
                        "type": "string",
                        "minLength": 1,
                        "maxLength": 10,
                        "pattern": "\\\\S"
                    },
                    "email": {
                        "type": "string",
                        "minLength": 1,
                        "maxLength": 200,
                        "pattern": "^[^\\\\s@]+@[^\\\\s@]+\\\\.[^\\\\s@]+$",
                        "format": "email"
                    }
                }
            },
            "maxItems": 5
        },
        "tags": {
            "type": [
                "array",
                "null"
            ],
            "items": {
                "type": "string",
                "maxLength": 50
            },
            "maxItems": 20
        },
        "version": {
            "type": [
                "integer",
                "null"
            ],
            "minimum": 0,
            "maximum": 9223372036854775807
        }
    },
    "title": "Workspace",
    "$schema": "https://json-schema.org/draft/2020-12/schema"
}
',
    'PUBLISHED',
    CURRENT_TIMESTAMP
FROM
    schema_definitions
WHERE
    scope_code = 'PLATFORM'
    AND code = 'workspace-resource';

INSERT IGNORE INTO
    schema_binding (
        version,
        identifier,
        resource_type,
        resource_code,
        schema_id,
        lifecycle_code,
        created_at,
        updated_at
    )
SELECT
    0,
    UUID(),
    'WORKSPACE',
    'workspace',
    id,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM
    schema_definitions
WHERE
    scope_code = 'PLATFORM'
    AND code = 'workspace-resource';
