-- Keep every identifier, version and association; MySQL updates incoming FK targets on rename.
DROP VIEW vw_dynamic_bundle_messages;
DROP VIEW vw_feature_runtime_config;
RENAME TABLE platform_microservices TO service_context_microservices,
             platform_features TO service_context_features,
             platform_feature_contexts TO service_context_groups,
             platform_feature_context_relations TO service_context_feature_groups;
ALTER TABLE service_context_feature_groups RENAME COLUMN feature_context_id TO group_id;
ALTER TABLE service_context_microservices RENAME INDEX fk_platform_services_lifecycle TO fk_service_context_microservices_lifecycle;
ALTER TABLE service_context_features RENAME INDEX fk_platform_features_microservice TO fk_service_context_features_microservice;
ALTER TABLE service_context_features RENAME INDEX fk_platform_features_lifecycle TO fk_service_context_features_lifecycle;
ALTER TABLE service_context_groups RENAME INDEX fk_platform_feature_contexts_lifecycle TO fk_service_context_groups_lifecycle;
ALTER TABLE service_context_feature_groups RENAME INDEX fk_platform_feature_context_relations_context TO fk_service_context_feature_groups_group;
ALTER TABLE service_context_microservices
 RENAME INDEX uk_platform_microservices_identifier TO uk_service_context_microservices_identifier,
 RENAME INDEX uk_platform_microservices_code TO uk_service_context_microservices_code,
 RENAME INDEX uk_platform_microservices_name TO uk_service_context_microservices_name;
ALTER TABLE service_context_microservices DROP FOREIGN KEY fk_platform_services_lifecycle, DROP CHECK ck_platform_microservices_code, ADD CONSTRAINT fk_service_context_microservices_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code), ADD CONSTRAINT ck_service_context_microservices_code CHECK (code REGEXP '^[a-z][a-z0-9]*(-[a-z0-9]+)*$');
ALTER TABLE service_context_features
 RENAME INDEX uk_platform_features_identifier TO uk_service_context_features_identifier,
 RENAME INDEX uk_platform_features_code TO uk_service_context_features_code,
 RENAME INDEX uk_platform_features_name TO uk_service_context_features_name;
ALTER TABLE service_context_features DROP FOREIGN KEY fk_platform_features_lifecycle, DROP CHECK ck_platform_features_code, ADD CONSTRAINT fk_service_context_features_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code), ADD CONSTRAINT ck_service_context_features_code CHECK (code REGEXP '^[a-z][a-z0-9]*(-[a-z0-9]+)*$');
ALTER TABLE service_context_groups
 RENAME INDEX uk_platform_feature_contexts_identifier TO uk_service_context_groups_identifier,
 RENAME INDEX uk_platform_feature_contexts_code TO uk_service_context_groups_code,
 RENAME INDEX uk_platform_feature_contexts_name TO uk_service_context_groups_name;
ALTER TABLE service_context_groups DROP FOREIGN KEY fk_platform_feature_contexts_lifecycle, DROP CHECK ck_platform_feature_contexts_code, ADD CONSTRAINT fk_service_context_groups_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code), ADD CONSTRAINT ck_service_context_groups_code CHECK (code REGEXP '^[a-z][a-z0-9]*(-[a-z0-9]+)*$');
ALTER TABLE service_context_features DROP FOREIGN KEY fk_platform_features_microservice,
 ADD CONSTRAINT fk_service_context_features_microservice FOREIGN KEY (microservice_id) REFERENCES service_context_microservices(id);
ALTER TABLE service_context_feature_groups
 DROP FOREIGN KEY fk_platform_feature_context_relations_feature,
 DROP FOREIGN KEY fk_platform_feature_context_relations_context,
 ADD CONSTRAINT fk_service_context_feature_groups_feature FOREIGN KEY (feature_id) REFERENCES service_context_features(id) ON DELETE CASCADE,
 ADD CONSTRAINT fk_service_context_feature_groups_group FOREIGN KEY (group_id) REFERENCES service_context_groups(id);
ALTER TABLE messages DROP FOREIGN KEY fk_messages_platform_microservice,
 ADD CONSTRAINT fk_messages_service_context_microservice FOREIGN KEY (microservice_id) REFERENCES service_context_microservices(id);
CREATE VIEW vw_service_context_feature_runtime_config AS
SELECT f.code                                                                                 AS feature_code,
       f.name                                                                                 AS feature_label,
       s.code                                                                                 AS microservice_code,
       s.name                                                                                 AS microservice_label,
       JSON_EXTRACT(f.settings, '$.quarantine.enabled') = true                                AS quarantine_enabled,
       CAST(JSON_UNQUOTE(JSON_EXTRACT(f.settings, '$.quarantine.retentionDays')) AS UNSIGNED) AS quarantine_retention_days,
       JSON_EXTRACT(f.settings, '$.quarantine.restoreAllowed') = true                         AS quarantine_restore_allowed,
       JSON_EXTRACT(f.settings, '$.audit.enabled') = true                                     AS audit_enabled,
       JSON_EXTRACT(f.settings, '$.audit.snapshotOnPurge') = true                             AS audit_snapshot_on_purge,
       JSON_EXTRACT(f.settings, '$.purge.enabled') = true                                     AS purge_enabled,
       f.lifecycle_code = 'ACTIVE'                                                            AS feature_active,
       s.lifecycle_code = 'ACTIVE'                                                            AS microservice_active
FROM service_context_features f
         JOIN service_context_microservices s ON s.id = f.microservice_id;


CREATE VIEW vw_dynamic_bundle_messages AS
SELECT CONCAT(s.code, '.', m.message_key) AS message_key,
 m.code AS code, m.http_status AS http_status, mt.locale AS locale,
 mt.title AS title, mt.detail AS message, mt.suggestion AS solution
FROM messages m JOIN service_context_microservices s ON s.id=m.microservice_id
JOIN message_translators mt ON mt.message_id=m.id
WHERE s.lifecycle_code='ACTIVE' AND m.lifecycle_code='ACTIVE' AND mt.lifecycle_code='ACTIVE';
UPDATE schema_definitions SET code='group-resource', name='Group Resource' WHERE scope_code='PLATFORM' AND code='context-resource';
UPDATE schema_configuration SET resource_type='SERVICE_CONTEXT', resource_code=CASE WHEN resource_code='context' THEN 'group' ELSE resource_code END WHERE resource_type='PLATFORM' AND resource_code IN ('microservice','feature','context');
INSERT INTO schema_versions(identifier,schema_id,schema_version,version_name,definition,status,created_at) SELECT UUID(),sd.id,v.latest+1,'Group Resource v3','{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Group request",
    "type": "object",
    "additionalProperties": false,
    "required": [
        "code",
        "name"
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
        "code": {
            "type": "string",
            "maxLength": 50,
            "pattern": "^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$"
        },
        "name": {
            "type": "string",
            "minLength": 1,
            "maxLength": 100,
            "pattern": "\\\\S"
        },
        "description": {
            "type": [
                "string",
                "null"
            ],
            "maxLength": 500
        }
    }
}
','PUBLISHED',CURRENT_TIMESTAMP FROM schema_definitions sd JOIN (SELECT schema_id,MAX(schema_version) latest FROM schema_versions GROUP BY schema_id) v ON v.schema_id=sd.id WHERE sd.scope_code='PLATFORM' AND sd.code='group-resource';
