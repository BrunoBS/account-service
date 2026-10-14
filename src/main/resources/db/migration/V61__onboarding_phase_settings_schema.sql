-- Publish the settings contract consumed by onboarding and catalog administration.
INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES (0, UUID(), 'PLATFORM', 'onboarding-phase-type-settings', 'Onboarding phase settings',
        'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 1, 'Onboarding phase settings v1',
    '{"$schema":"https://json-schema.org/draft/2020-12/schema","title":"Onboarding phase settings","type":"object","additionalProperties":true,"properties":{"orientation":{"type":["string","null"]}}}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'onboarding-phase-type-settings';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'CATALOG', 'onboarding-phase-type', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'onboarding-phase-type-settings';
