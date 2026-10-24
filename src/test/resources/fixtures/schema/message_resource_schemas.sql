-- Publish shared create/update request contracts for message and translation.

INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES (0, UUID(), 'PLATFORM', 'message-resource', 'Message request', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 1, 'message v1',
    '{"$schema": "https://json-schema.org/draft/2020-12/schema", "title": "Message request", "type": "object", "additionalProperties": false, "required": ["microserviceIdentifier", "messageKey", "code", "httpStatus"], "properties": {"version": {"type": ["integer", "null"], "minimum": 0, "maximum": 9223372036854775807}, "microserviceIdentifier": {"type": "string", "format": "uuid", "minLength": 36, "maxLength": 36}, "messageKey": {"type": "string", "minLength": 1, "maxLength": 255, "pattern": "^[a-z0-9][a-z0-9._-]{0,254}$"}, "code": {"type": "string", "minLength": 3, "maxLength": 50, "pattern": "^[A-Z][A-Z0-9-]{2,49}$"}, "httpStatus": {"type": "integer", "minimum": 100, "maximum": 599}, "observation": {"type": ["string", "null"], "maxLength": 500}, "translations": {"type": ["array", "null"], "maxItems": 100, "items": {"type": "object", "additionalProperties": false, "required": ["locale", "title", "detail", "suggestion"], "properties": {"version": {"type": ["integer", "null"], "minimum": 0, "maximum": 9223372036854775807}, "locale": {"type": "string", "minLength": 2, "maxLength": 35, "pattern": "^[A-Za-z0-9]+(-[A-Za-z0-9]+)*$"}, "title": {"type": "string", "minLength": 1, "maxLength": 150, "pattern": "\\\\S"}, "detail": {"type": "string", "minLength": 1, "maxLength": 1000, "pattern": "\\\\S"}, "suggestion": {"type": "string", "minLength": 1, "maxLength": 1000, "pattern": "\\\\S"}}}}}, "allOf": [{"if": {"required": ["version"], "properties": {"version": {"type": "integer"}}}, "then": {"not": {"required": ["translations"]}}}]}',
    'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'message-resource';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'MESSAGE', 'message', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'message-resource';

INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES (0, UUID(), 'PLATFORM', 'message-translation-resource', 'Message translation request', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 1, 'message-translation v1',
    '{"$schema": "https://json-schema.org/draft/2020-12/schema", "title": "Message translation request", "type": "object", "additionalProperties": false, "required": ["locale", "title", "detail", "suggestion"], "properties": {"version": {"type": ["integer", "null"], "minimum": 0, "maximum": 9223372036854775807}, "locale": {"type": "string", "minLength": 2, "maxLength": 35, "pattern": "^[A-Za-z0-9]+(-[A-Za-z0-9]+)*$"}, "title": {"type": "string", "minLength": 1, "maxLength": 150, "pattern": "\\\\S"}, "detail": {"type": "string", "minLength": 1, "maxLength": 1000, "pattern": "\\\\S"}, "suggestion": {"type": "string", "minLength": 1, "maxLength": 1000, "pattern": "\\\\S"}}}',
    'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'message-translation-resource';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'MESSAGE', 'message-translation', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'message-translation-resource';
