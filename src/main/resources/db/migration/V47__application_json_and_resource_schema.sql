-- Persist structured settings and align the published contract with the reference JSON Schema.
ALTER TABLE applications
    MODIFY settings JSON NOT NULL,
    MODIFY name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;

INSERT INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), sd.id, 2, 'Application Resource v2',
    '
{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Application",
    "type": "object",
    "additionalProperties": false,
    "required": [
        "name",
        "alias",
        "acronym",
        "applicationScope"
    ],
    "properties": {
        "version": {
            "type": "integer",
            "minimum": 0,
            "maximum": 9223372036854775807
        },
        "name": {
            "type": "string",
            "minLength": 3,
            "maxLength": 100,
            "pattern": "\\\\S"
        },
        "alias": {
            "type": "string",
            "minLength": 1,
            "maxLength": 100,
            "pattern": "\\\\S"
        },
        "acronym": {
            "type": "string",
            "minLength": 1,
            "maxLength": 20,
            "pattern": "\\\\S"
        },
        "applicationScope": {
            "type": "string",
            "enum": [
                "BACKEND",
                "FRONTEND",
                "SHARED"
            ]
        },
        "authorizerGroup": {
            "type": "string",
            "minLength": 1,
            "maxLength": 255
        },
        "settings": {
            "type": "object",
            "additionalProperties": true
        },
        "tags": {
            "type": "array",
            "maxItems": 20,
            "items": {
                "type": "string",
                "minLength": 1,
                "maxLength": 150
            }
        }
    }
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions sd
WHERE sd.scope_code = 'PLATFORM' AND sd.code = 'application-resource';
