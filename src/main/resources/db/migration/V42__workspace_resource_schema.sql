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
    schema_configuration (
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
