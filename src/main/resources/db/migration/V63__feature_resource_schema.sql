-- Separate the feature HTTP contract from per-feature settings schemas.
INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES (0, UUID(), 'PLATFORM', 'feature-resource', 'Feature Resource',
        'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 1, 'Feature Resource v1',
    '{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Feature request",
    "type": "object",
    "additionalProperties": false,
    "required": [
        "code",
        "name",
        "settings",
        "microserviceIdentifier"
    ],
    "properties": {
        "version": {
            "type": [
                "integer",
                "null"
            ],
            "minimum": 0,
            "maximum": 9223372036854775807
        },
        "code": {
            "type": "string",
            "maxLength": 50,
            "pattern": "^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$"
        },
        "name": {
            "type": "string",
            "minLength": 1,
            "maxLength": 100,
            "pattern": "\\\\S"
        },
        "description": {
            "type": [
                "string",
                "null"
            ],
            "maxLength": 500
        },
        "settings": {
            "type": "object"
        },
        "microserviceIdentifier": {
            "type": "string",
            "format": "uuid"
        },
        "shareable": {
            "type": "boolean",
            "default": false
        }
    }
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'feature-resource';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'PLATFORM', 'feature', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'feature-resource';
