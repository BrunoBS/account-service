-- Align Workspace and Application attribute limits; add Application description.
ALTER TABLE workspaces
    MODIFY name VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    MODIFY authorizer_group VARCHAR(10) NULL,
    MODIFY email_group VARCHAR(100) NOT NULL;
ALTER TABLE workspace_approvers MODIFY email VARCHAR(100) NOT NULL;
ALTER TABLE applications
    MODIFY name VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    MODIFY alias VARCHAR(50) NOT NULL,
    MODIFY authorizer_group VARCHAR(10) NULL,
    ADD COLUMN description VARCHAR(200) NULL AFTER alias;
ALTER TABLE workspace_tag MODIFY name VARCHAR(50) COLLATE utf8mb4_unicode_ci NOT NULL;
ALTER TABLE application_tag MODIFY name VARCHAR(50) COLLATE utf8mb4_unicode_ci NOT NULL;

INSERT INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 4, 'Application Resource v4',
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
            "maxLength": 50,
            "pattern": "\\\\S"
        },
        "alias": {
            "type": "string",
            "minLength": 1,
            "maxLength": 50,
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
            "maxLength": 10
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
        },
        "description": {
            "type": [
                "string",
                "null"
            ],
            "maxLength": 200
        }
    }
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code='PLATFORM' AND code='application-resource';

INSERT INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 2, 'Workspace Resource v2',
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
            "maxLength": 50,
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
            "maxLength": 10
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
            "maxLength": 100,
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
                        "maxLength": 100,
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
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code='PLATFORM' AND code='workspace-resource';
