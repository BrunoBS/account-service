-- Publish the global segment/category contracts using existing PLATFORM schema infrastructure.
INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES (0, UUID(), 'PLATFORM', 'segment-resource', 'Segment Resource', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 1, 'Segment Resource v1', '{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Segment request",
    "type": "object",
    "additionalProperties": false,
    "required": [
        "code",
        "name"
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
            "maxLength": 100,
            "pattern": "^[a-z0-9]+(?:-[a-z0-9]+)*$"
        },
        "name": {
            "type": "string",
            "minLength": 1,
            "maxLength": 150,
            "pattern": "\\\\S"
        }
    }
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'segment-resource';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'PLATFORM', 'segment', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'segment-resource';

INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES (0, UUID(), 'PLATFORM', 'segment-category-resource', 'Segment Category Resource', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 1, 'Segment Category Resource v1', '{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Segment Category request",
    "type": "object",
    "additionalProperties": false,
    "required": [
        "code",
        "name"
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
            "maxLength": 100,
            "pattern": "^[a-z0-9]+(?:-[a-z0-9]+)*$"
        },
        "name": {
            "type": "string",
            "minLength": 1,
            "maxLength": 150,
            "pattern": "\\\\S"
        }
    }
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'segment-category-resource';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'PLATFORM', 'segment-category', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'segment-category-resource';
