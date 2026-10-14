-- Align equivalent Application attributes with the Workspace storage/schema standard.
ALTER TABLE applications
    DROP FOREIGN KEY fk_applications_scope,
    DROP FOREIGN KEY fk_applications_lifecycle;

ALTER TABLE applications
    MODIFY name VARCHAR(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    MODIFY alias VARCHAR(80) NOT NULL,
    MODIFY acronym VARCHAR(5) NOT NULL,
    MODIFY authorizer_group VARCHAR(20) NULL,
    MODIFY application_scope_code VARCHAR(20) NOT NULL,
    MODIFY lifecycle_code VARCHAR(20) NOT NULL,
    MODIFY settings JSON NOT NULL,
    ADD CONSTRAINT fk_applications_scope FOREIGN KEY (application_scope_code) REFERENCES type_application_scopes(code),
    ADD CONSTRAINT fk_applications_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code);

-- System tags contain names/aliases up to 80 characters; manual tags are limited to 50 by the schema.
ALTER TABLE application_tag
    MODIFY name VARCHAR(80) COLLATE utf8mb4_unicode_ci NOT NULL;

INSERT INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 3, 'Application Resource v3',
    '{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Application",
    "type": "object",
    "additionalProperties": false,
    "required": [
        "name",
        "alias",
        "acronym",
        "applicationScope",
        "settings"
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
        "name": {
            "type": "string",
            "minLength": 3,
            "maxLength": 80,
            "pattern": "\\\\S"
        },
        "alias": {
            "type": "string",
            "minLength": 1,
            "maxLength": 80,
            "pattern": "\\\\S"
        },
        "acronym": {
            "type": "string",
            "minLength": 1,
            "maxLength": 5,
            "pattern": "\\\\S"
        },
        "applicationScope": {
            "type": "string",
            "enum": [
                "BACKEND",
                "FRONTEND",
                "SHARED"
            ],
            "minLength": 1,
            "maxLength": 20
        },
        "authorizerGroup": {
            "type": [
                "string",
                "null"
            ],
            "maxLength": 20
        },
        "settings": {
            "type": "object",
            "additionalProperties": false,
            "required": [
                "language",
                "infrastructure"
            ],
            "properties": {
                "language": {
                    "type": "string",
                    "enum": [
                        "JAVA",
                        "C#",
                        "NODE",
                        "GO"
                    ]
                },
                "infrastructure": {
                    "type": "string",
                    "enum": [
                        "VM",
                        "ECS",
                        "EKS"
                    ]
                }
            }
        },
        "tags": {
            "type": [
                "array",
                "null"
            ],
            "maxItems": 20,
            "items": {
                "type": "string",
                "maxLength": 50
            }
        }
    }
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions
WHERE scope_code = 'PLATFORM' AND code = 'application-resource';
