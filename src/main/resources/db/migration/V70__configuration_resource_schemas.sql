INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES (0, UUID(), 'PLATFORM', 'configuration-resource', 'configuration-resource', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 1, 'configuration-resource v1', '{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Configuration request",
    "type": "object",
    "additionalProperties": false,
    "required": [
        "type",
        "value"
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
        "microserviceIdentifier": {
            "type": "string",
            "format": "uuid",
            "maxLength": 36
        },
        "key": {
            "type": "string",
            "maxLength": 80,
            "pattern": "^[A-Za-z0-9]+(?:[._-][A-Za-z0-9]+)*$"
        },
        "description": {
            "type": [
                "string",
                "null"
            ],
            "maxLength": 200
        },
        "type": {
            "type": "string",
            "enum": [
                "CONFIGURATION",
                "EXPRESSION"
            ]
        },
        "value": {}
    },
    "oneOf": [
        {
            "required": [
                "microserviceIdentifier",
                "key"
            ],
            "properties": {
                "version": {
                    "type": "null"
                }
            }
        },
        {
            "required": [
                "version"
            ],
            "properties": {
                "version": {
                    "type": "integer"
                },
                "microserviceIdentifier": false,
                "key": false
            }
        }
    ]
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'configuration-resource';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'PLATFORM', 'configuration', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'configuration-resource';

INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES (0, UUID(), 'PLATFORM', 'configuration-expression', 'configuration-expression', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 1, 'configuration-expression v1', '{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Configuration Expression V1",
    "type": "object",
    "required": [
        "operator",
        "valueType",
        "value"
    ],
    "properties": {
        "operator": {
            "enum": [
                "EQUALS",
                "NOT_EQUALS",
                "CONTAINS",
                "NOT_CONTAINS",
                "GREATER_THAN",
                "GREATER_THAN_OR_EQUALS",
                "LESS_THAN",
                "LESS_THAN_OR_EQUALS"
            ]
        },
        "valueType": {
            "enum": [
                "STRING",
                "INTEGER",
                "LONG",
                "DECIMAL"
            ]
        },
        "value": {}
    },
    "allOf": [
        {
            "if": {
                "properties": {
                    "operator": {
                        "enum": [
                            "CONTAINS",
                            "NOT_CONTAINS"
                        ]
                    }
                },
                "required": [
                    "operator"
                ]
            },
            "then": {
                "properties": {
                    "value": {
                        "type": "array",
                        "minItems": 1,
                        "uniqueItems": true
                    }
                }
            },
            "else": {
                "properties": {
                    "value": {
                        "not": {
                            "type": "array"
                        }
                    }
                }
            }
        },
        {
            "if": {
                "properties": {
                    "operator": {
                        "enum": [
                            "GREATER_THAN",
                            "GREATER_THAN_OR_EQUALS",
                            "LESS_THAN",
                            "LESS_THAN_OR_EQUALS"
                        ]
                    }
                },
                "required": [
                    "operator"
                ]
            },
            "then": {
                "properties": {
                    "valueType": {
                        "enum": [
                            "INTEGER",
                            "LONG",
                            "DECIMAL"
                        ]
                    }
                }
            }
        },
        {
            "if": {
                "properties": {
                    "valueType": {
                        "const": "STRING"
                    }
                },
                "required": [
                    "valueType"
                ]
            },
            "then": {
                "properties": {
                    "value": {
                        "oneOf": [
                            {
                                "type": "string"
                            },
                            {
                                "type": "array",
                                "items": {
                                    "type": "string"
                                }
                            }
                        ]
                    }
                }
            }
        },
        {
            "if": {
                "properties": {
                    "valueType": {
                        "const": "INTEGER"
                    }
                },
                "required": [
                    "valueType"
                ]
            },
            "then": {
                "properties": {
                    "value": {
                        "oneOf": [
                            {
                                "type": "integer",
                                "minimum": -2147483648,
                                "maximum": 2147483647
                            },
                            {
                                "type": "array",
                                "items": {
                                    "type": "integer",
                                    "minimum": -2147483648,
                                    "maximum": 2147483647
                                }
                            }
                        ]
                    }
                }
            }
        },
        {
            "if": {
                "properties": {
                    "valueType": {
                        "const": "LONG"
                    }
                },
                "required": [
                    "valueType"
                ]
            },
            "then": {
                "properties": {
                    "value": {
                        "oneOf": [
                            {
                                "type": "integer",
                                "minimum": -9223372036854775808,
                                "maximum": 9223372036854775807
                            },
                            {
                                "type": "array",
                                "items": {
                                    "type": "integer",
                                    "minimum": -9223372036854775808,
                                    "maximum": 9223372036854775807
                                }
                            }
                        ]
                    }
                }
            }
        },
        {
            "if": {
                "properties": {
                    "valueType": {
                        "const": "DECIMAL"
                    }
                },
                "required": [
                    "valueType"
                ]
            },
            "then": {
                "properties": {
                    "value": {
                        "oneOf": [
                            {
                                "type": "number"
                            },
                            {
                                "type": "array",
                                "items": {
                                    "type": "number"
                                }
                            }
                        ]
                    }
                }
            }
        }
    ],
    "additionalProperties": false
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'configuration-expression';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'PLATFORM', 'configuration-expression', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'configuration-expression';

INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES (0, UUID(), 'PLATFORM', 'configuration-type-settings', 'configuration-type-settings', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 1, 'configuration-type-settings v1', '{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "configuration-type settings",
    "type": "object",
    "additionalProperties": false
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'configuration-type-settings';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'CATALOG', 'configuration-type', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'configuration-type-settings';

INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES (0, UUID(), 'PLATFORM', 'configuration-status-type-settings', 'configuration-status-type-settings', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 1, 'configuration-status-type-settings v1', '{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "configuration-status-type settings",
    "type": "object",
    "additionalProperties": false
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'configuration-status-type-settings';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'CATALOG', 'configuration-status-type', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'configuration-status-type-settings';
