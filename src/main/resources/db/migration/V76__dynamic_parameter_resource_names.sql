-- Rename schema identities/bindings without recreating them or modifying published definitions.
UPDATE schema_definitions
SET name = CASE code
        WHEN 'configuration-resource' THEN 'dynamic-parameter-resource'
        WHEN 'configuration-expression' THEN 'dynamic-parameter-expression'
        WHEN 'configuration-publication' THEN 'dynamic-parameter-publication'
        WHEN 'configuration-type-settings' THEN 'parameter-key-type-settings'
        WHEN 'configuration-status-type-settings' THEN 'parameter-key-status-type-settings'
    END,
    code = CASE code
        WHEN 'configuration-resource' THEN 'dynamic-parameter-resource'
        WHEN 'configuration-expression' THEN 'dynamic-parameter-expression'
        WHEN 'configuration-publication' THEN 'dynamic-parameter-publication'
        WHEN 'configuration-type-settings' THEN 'parameter-key-type-settings'
        WHEN 'configuration-status-type-settings' THEN 'parameter-key-status-type-settings'
    END
WHERE scope_code = 'PLATFORM' AND code IN ('configuration-resource', 'configuration-expression',
    'configuration-publication', 'configuration-type-settings', 'configuration-status-type-settings');

UPDATE schema_configuration
SET resource_code = CASE resource_code
        WHEN 'configuration' THEN 'dynamic-parameter'
        WHEN 'configuration-expression' THEN 'dynamic-parameter-expression'
        WHEN 'configuration-publication' THEN 'dynamic-parameter-publication'
        WHEN 'configuration-type' THEN 'parameter-key-type'
        WHEN 'configuration-status-type' THEN 'parameter-key-status-type'
    END
WHERE (resource_type = 'PLATFORM' AND resource_code IN ('configuration', 'configuration-expression', 'configuration-publication'))
    OR (resource_type = 'CATALOG' AND resource_code IN ('configuration-type', 'configuration-status-type'));

-- Publish display metadata under the new names, retaining all prior schema versions unchanged.
INSERT INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), published.schema_id, published.schema_version + 1,
       CONCAT(definition.code, ' v', published.schema_version + 1),
       JSON_SET(published.definition, '$.title', CASE definition.code
           WHEN 'dynamic-parameter-resource' THEN 'Parameter key request'
           WHEN 'dynamic-parameter-publication' THEN 'Parameter key publication'
           WHEN 'parameter-key-type-settings' THEN 'Parameter key type settings'
           WHEN 'parameter-key-status-type-settings' THEN 'Parameter key status type settings'
       END), 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_versions published
JOIN schema_definitions definition ON definition.id = published.schema_id
JOIN (
    SELECT schema_id, MAX(schema_version) AS schema_version
    FROM schema_versions WHERE status = 'PUBLISHED' GROUP BY schema_id
) latest ON latest.schema_id = published.schema_id AND latest.schema_version = published.schema_version
WHERE definition.scope_code = 'PLATFORM'
  AND definition.code IN ('dynamic-parameter-resource', 'dynamic-parameter-publication',
                          'parameter-key-type-settings', 'parameter-key-status-type-settings')
  AND published.status = 'PUBLISHED';
