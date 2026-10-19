-- Keep immutable historical schema versions while publishing their canonical domain titles.
UPDATE schema_definitions
SET code = 'feature-group-feature-resource', name = 'Feature Group Feature Resource'
WHERE scope_code = 'PLATFORM' AND code = 'feature-context-resource';

UPDATE schema_configuration
SET resource_type = 'SERVICE_CONTEXT', resource_code = 'feature-group-feature'
WHERE resource_type = 'PLATFORM' AND resource_code = 'feature-context';

INSERT INTO schema_versions(identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), definition.id, latest.schema_version + 1,
       CONCAT('Feature Group Feature Resource v', latest.schema_version + 1),
       JSON_SET(published.definition, '$.title', 'Feature Group associations request'),
       'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions definition
JOIN (
    SELECT schema_id, MAX(schema_version) AS schema_version
    FROM schema_versions
    WHERE status = 'PUBLISHED'
    GROUP BY schema_id
) latest ON latest.schema_id = definition.id
JOIN schema_versions published
  ON published.schema_id = latest.schema_id
 AND published.schema_version = latest.schema_version
WHERE definition.scope_code = 'PLATFORM'
  AND definition.code = 'feature-group-feature-resource';

INSERT INTO schema_versions(identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), definition.id, latest.schema_version + 1,
       CONCAT('Category Resource v', latest.schema_version + 1),
       JSON_SET(published.definition, '$.title', 'Category request'),
       'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions definition
JOIN (
    SELECT schema_id, MAX(schema_version) AS schema_version
    FROM schema_versions
    WHERE status = 'PUBLISHED'
    GROUP BY schema_id
) latest ON latest.schema_id = definition.id
JOIN schema_versions published
  ON published.schema_id = latest.schema_id
 AND published.schema_version = latest.schema_version
WHERE definition.scope_code = 'PLATFORM'
  AND definition.code = 'category-resource';
