-- One request schema for Publisher creation and update, including conditional settings.
ALTER TABLE publishers MODIFY description VARCHAR(200) NOT NULL;
INSERT IGNORE INTO schema_definitions (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES (0, UUID(), 'PLATFORM', 'publisher-resource', 'Publisher Resource', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT IGNORE INTO schema_versions (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 1, 'Publisher Resource v1', '{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Publisher request",
    "type": "object",
    "additionalProperties": false,
    "required": [
        "name",
        "description",
        "scope",
        "settings",
        "code"
    ],
    "properties": {
        "code": {
            "type": "string",
            "pattern": "^[A-Z][A-Z0-9_]{0,39}$",
            "maxLength": 40
        },
        "name": {
            "type": "string",
            "minLength": 3,
            "maxLength": 50,
            "pattern": "\\\\S"
        },
        "description": {
            "type": "string",
            "minLength": 1,
            "maxLength": 200,
            "pattern": "\\\\S"
        },
        "scope": {
            "type": "string",
            "enum": [
                "WORKSPACE",
                "APPLICATION"
            ],
            "maxLength": 50
        },
        "deprecated": {
            "type": "boolean"
        },
        "settings": {
            "type": "object"
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
    "allOf": [
        {
            "if": {
                "properties": {
                    "code": {
                        "const": "KAFKA"
                    }
                },
                "required": [
                    "code"
                ]
            },
            "then": {
                "properties": {
                    "settings": {
                        "type": "object",
                        "required": [
                            "topic",
                            "address"
                        ],
                        "properties": {
                            "topic": {
                                "type": "string",
                                "minLength": 1,
                                "maxLength": 249,
                                "pattern": "\\\\S"
                            },
                            "address": {
                                "type": "string",
                                "minLength": 1,
                                "maxLength": 1000,
                                "pattern": "\\\\S"
                            }
                        },
                        "additionalProperties": false
                    }
                }
            }
        },
        {
            "if": {
                "properties": {
                    "code": {
                        "const": "APPCONFIG"
                    }
                },
                "required": [
                    "code"
                ]
            },
            "then": {
                "properties": {
                    "settings": {
                        "type": "object",
                        "required": [
                            "arn",
                            "name"
                        ],
                        "properties": {
                            "arn": {
                                "type": "string",
                                "minLength": 1,
                                "maxLength": 2048,
                                "pattern": "\\\\S"
                            },
                            "name": {
                                "type": "string",
                                "minLength": 1,
                                "maxLength": 64,
                                "pattern": "\\\\S"
                            }
                        },
                        "additionalProperties": false
                    }
                }
            }
        },
        {
            "if": {
                "properties": {
                    "code": {
                        "const": "HTTP"
                    }
                },
                "required": [
                    "code"
                ]
            },
            "then": {
                "properties": {
                    "settings": {
                        "type": "object",
                        "required": [
                            "clientId",
                            "timeout"
                        ],
                        "properties": {
                            "clientId": {
                                "type": "string",
                                "minLength": 1,
                                "maxLength": 100,
                                "pattern": "\\\\S"
                            },
                            "timeout": {
                                "type": "integer",
                                "minimum": 1
                            }
                        },
                        "additionalProperties": false
                    }
                }
            }
        }
    ]
}', 'PUBLISHED', CURRENT_TIMESTAMP FROM schema_definitions WHERE scope_code='PLATFORM' AND code='publisher-resource';
INSERT IGNORE INTO schema_configuration (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'PUBLISHER', 'publisher', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM schema_definitions WHERE scope_code='PLATFORM' AND code='publisher-resource';
