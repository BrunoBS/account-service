-- Rename the Service Context group aggregate and its N:N join table without recreating data.
RENAME TABLE service_context_groups TO feature_group,
             service_context_feature_groups TO feature_group_feature;
ALTER TABLE feature_group_feature RENAME COLUMN group_id TO feature_group_id;

ALTER TABLE feature_group
    DROP FOREIGN KEY fk_service_context_groups_lifecycle,
    DROP CHECK ck_service_context_groups_code;
ALTER TABLE feature_group_feature
    DROP FOREIGN KEY fk_service_context_feature_groups_feature,
    DROP FOREIGN KEY fk_service_context_feature_groups_group;

ALTER TABLE feature_group
    RENAME INDEX fk_service_context_groups_lifecycle TO fk_feature_group_lifecycle,
    RENAME INDEX uk_service_context_groups_identifier TO uk_feature_group_identifier,
    RENAME INDEX uk_service_context_groups_code TO uk_feature_group_code,
    RENAME INDEX uk_service_context_groups_name TO uk_feature_group_name;
ALTER TABLE feature_group_feature
    RENAME INDEX fk_service_context_feature_groups_group TO fk_feature_group_feature_feature_group;

ALTER TABLE feature_group
    ADD CONSTRAINT fk_feature_group_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code),
    ADD CONSTRAINT ck_feature_group_code CHECK (code REGEXP '^[a-z][a-z0-9]*(-[a-z0-9]+)*$');
ALTER TABLE feature_group_feature
    ADD CONSTRAINT fk_feature_group_feature_feature FOREIGN KEY (feature_id) REFERENCES service_context_features(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_feature_group_feature_feature_group FOREIGN KEY (feature_group_id) REFERENCES feature_group(id);

UPDATE schema_definitions
SET code = 'feature-group-resource', name = 'Feature Group Resource'
WHERE scope_code = 'PLATFORM' AND code = 'group-resource';
UPDATE schema_configuration
SET resource_code = 'feature-group'
WHERE resource_type = 'SERVICE_CONTEXT' AND resource_code = 'group';

INSERT INTO schema_versions(identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), definition.id, latest.schema_version + 1,
       CONCAT('Feature Group Resource v', latest.schema_version + 1),
       JSON_SET(published.definition, '$.title', 'Feature Group request'),
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
  AND definition.code = 'feature-group-resource';
