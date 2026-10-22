-- Rename technical codes only after checking collisions; association FKs use immutable ids.
-- SIGNAL guards execute before any persistent DML. Existing audit snapshots remain historical.
DELIMITER $$
CREATE PROCEDURE validate_publisher_code_rename_v86()
BEGIN
    IF EXISTS (SELECT 1 FROM publishers a JOIN publishers b ON a.code='HTTP' AND b.code='WEB_SOCKET')
       OR EXISTS (SELECT 1 FROM publishers a JOIN publishers b ON a.code='APPCONFIG' AND b.code='APP_CONFIG')
       OR EXISTS (SELECT 1 FROM schema_binding a JOIN schema_binding b ON a.resource_type='PUBLISHER' AND b.resource_type='PUBLISHER' AND a.resource_code='HTTP' AND b.resource_code='WEB_SOCKET')
       OR EXISTS (SELECT 1 FROM schema_binding a JOIN schema_binding b ON a.resource_type='PUBLISHER' AND b.resource_type='PUBLISHER' AND a.resource_code='APPCONFIG' AND b.resource_code='APP_CONFIG') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Publisher code rename collision; reconcile catalog before V86';
    END IF;
    IF EXISTS (
        SELECT 1 FROM schema_versions v JOIN schema_definitions d ON d.id=v.schema_id
        WHERE d.scope_code='PLATFORM' AND d.code='publisher-resource' AND v.status='PUBLISHED'
          AND v.schema_version=(SELECT MAX(p.schema_version) FROM schema_versions p WHERE p.schema_id=v.schema_id AND p.status='PUBLISHED')
          AND (COALESCE(JSON_UNQUOTE(JSON_EXTRACT(v.definition,'$.allOf[1].if.properties.code.const')),'') <> 'APPCONFIG'
            OR COALESCE(JSON_UNQUOTE(JSON_EXTRACT(v.definition,'$.allOf[2].if.properties.code.const')),'') <> 'HTTP')
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Publisher schema layout differs from baseline; review V86 mapping';
    END IF;
END$$
DELIMITER ;
CALL validate_publisher_code_rename_v86();
DROP PROCEDURE validate_publisher_code_rename_v86;
UPDATE publishers SET code = CASE code WHEN 'HTTP' THEN 'WEB_SOCKET' WHEN 'APPCONFIG' THEN 'APP_CONFIG' END,
    version=version+1, updated_at=NOW(6) WHERE code IN ('HTTP','APPCONFIG');
-- Preserve the ids and schema ownership of existing per-publisher settings bindings.
UPDATE schema_binding SET resource_code=CASE resource_code WHEN 'HTTP' THEN 'WEB_SOCKET' WHEN 'APPCONFIG' THEN 'APP_CONFIG' END,
 version=version+1,updated_at=NOW() WHERE resource_type='PUBLISHER' AND resource_code IN ('HTTP','APPCONFIG');
-- Publish a successor of the latest cadastral schema, preserving administrated restrictions.
INSERT INTO schema_versions (identifier,schema_id,schema_version,version_name,definition,status,created_at)
SELECT UUID(),v.schema_id, (SELECT COALESCE(MAX(x.schema_version),0)+1 FROM schema_versions x WHERE x.schema_id=v.schema_id),
 'Publisher technical codes v86',
 JSON_SET(v.definition,
   '$.properties.code.enum',JSON_ARRAY('KAFKA','WEB_SOCKET','APP_CONFIG'),
   '$.allOf[1].if.properties.code.const','APP_CONFIG',
   '$.allOf[2].if.properties.code.const','WEB_SOCKET'),
 'PUBLISHED',NOW()
FROM schema_versions v JOIN schema_definitions d ON d.id=v.schema_id
WHERE d.scope_code='PLATFORM' AND d.code='publisher-resource' AND v.status='PUBLISHED'
 AND v.schema_version=(SELECT MAX(p.schema_version) FROM schema_versions p WHERE p.schema_id=v.schema_id AND p.status='PUBLISHED');

INSERT INTO schema_definitions (version,identifier,scope_code,code,name,lifecycle_code,created_at,updated_at)
VALUES (0,UUID(),'PLATFORM','workspace-environment-configuration','workspace-environment-configuration','ACTIVE',NOW(),NOW());
INSERT INTO schema_versions (identifier,schema_id,schema_version,version_name,definition,status,created_at)
SELECT UUID(),id,1,'workspace-environment-configuration v1','{"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object","additionalProperties":false,"required":["settings","publishers"],"properties":{"version":{"type":["integer","null"],"minimum":0,"maximum":9223372036854775807},"settings":{"type":"object","additionalProperties":true},"publishers":{"type":"array","minItems":1,"items":{"type":"object","additionalProperties":false,"required":["publisherIdentifier","sortOrder","parameters"],"properties":{"publisherIdentifier":{"type":"string","format":"uuid"},"sortOrder":{"type":"integer","minimum":1,"maximum":2147483647},"parameters":{"type":"object","additionalProperties":true}}}},"environmentIdentifier":{"type":"string","format":"uuid"}},"if":{"required":["version"],"properties":{"version":{"type":"integer"}}},"then":{"not":{"required":["environmentIdentifier"]}},"else":{"required":["environmentIdentifier"]}}','PUBLISHED',NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='workspace-environment-configuration';
INSERT INTO schema_binding (version,identifier,resource_type,resource_code,schema_id,lifecycle_code,created_at,updated_at)
SELECT 0,UUID(),'PLATFORM','workspace-environment-configuration',id,'ACTIVE',NOW(),NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='workspace-environment-configuration';

INSERT INTO schema_definitions (version,identifier,scope_code,code,name,lifecycle_code,created_at,updated_at)
VALUES (0,UUID(),'PLATFORM','application-environment-configuration','application-environment-configuration','ACTIVE',NOW(),NOW());
INSERT INTO schema_versions (identifier,schema_id,schema_version,version_name,definition,status,created_at)
SELECT UUID(),id,1,'application-environment-configuration v1','{"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object","additionalProperties":false,"required":["settings"],"properties":{"version":{"type":["integer","null"],"minimum":0,"maximum":9223372036854775807},"settings":{"type":"object","additionalProperties":true},"publishers":{"type":"array","minItems":1,"items":{"type":"object","additionalProperties":false,"required":["publisherIdentifier","sortOrder","parameters"],"properties":{"publisherIdentifier":{"type":"string","format":"uuid"},"sortOrder":{"type":"integer","minimum":1,"maximum":2147483647},"parameters":{"type":"object","additionalProperties":true}}}},"environmentIdentifier":{"type":"string","format":"uuid"},"copyWorkspacePublishers":{"type":"boolean","default":false}},"allOf":[{"if":{"required":["version"],"properties":{"version":{"type":"integer"}}},"then":{"not":{"required":["environmentIdentifier"]}},"else":{"required":["environmentIdentifier"]}},{"if":{"required":["copyWorkspacePublishers"],"properties":{"copyWorkspacePublishers":{"const":true}}},"then":{"not":{"required":["publishers"]}},"else":{"required":["publishers"]}}]}','PUBLISHED',NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='application-environment-configuration';
INSERT INTO schema_binding (version,identifier,resource_type,resource_code,schema_id,lifecycle_code,created_at,updated_at)
SELECT 0,UUID(),'PLATFORM','application-environment-configuration',id,'ACTIVE',NOW(),NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='application-environment-configuration';

INSERT INTO schema_definitions (version,identifier,scope_code,code,name,lifecycle_code,created_at,updated_at)
VALUES (0,UUID(),'PLATFORM','environment-configuration-publisher-web-socket','environment-configuration-publisher-web-socket','ACTIVE',NOW(),NOW());
INSERT INTO schema_versions (identifier,schema_id,schema_version,version_name,definition,status,created_at)
SELECT UUID(),id,1,'environment-configuration-publisher-web-socket v1','{"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object","additionalProperties":false,"required":["publisherIdentifier","sortOrder","parameters"],"properties":{"publisherIdentifier":{"type":"string","format":"uuid","pattern":"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"},"sortOrder":{"type":"integer","minimum":1,"maximum":2147483647},"parameters":{"type":"object","additionalProperties":true}}}','PUBLISHED',NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='environment-configuration-publisher-web-socket';
INSERT INTO schema_binding (version,identifier,resource_type,resource_code,schema_id,lifecycle_code,created_at,updated_at)
SELECT 0,UUID(),'PLATFORM','environment-configuration-publisher-web-socket',id,'ACTIVE',NOW(),NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='environment-configuration-publisher-web-socket';

INSERT INTO schema_definitions (version,identifier,scope_code,code,name,lifecycle_code,created_at,updated_at)
VALUES (0,UUID(),'PLATFORM','environment-configuration-publisher-kafka','environment-configuration-publisher-kafka','ACTIVE',NOW(),NOW());
INSERT INTO schema_versions (identifier,schema_id,schema_version,version_name,definition,status,created_at)
SELECT UUID(),id,1,'environment-configuration-publisher-kafka v1','{"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object","additionalProperties":false,"required":["publisherIdentifier","sortOrder","parameters"],"properties":{"publisherIdentifier":{"type":"string","format":"uuid","pattern":"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"},"sortOrder":{"type":"integer","minimum":1,"maximum":2147483647},"parameters":{"type":"object","additionalProperties":true}}}','PUBLISHED',NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='environment-configuration-publisher-kafka';
INSERT INTO schema_binding (version,identifier,resource_type,resource_code,schema_id,lifecycle_code,created_at,updated_at)
SELECT 0,UUID(),'PLATFORM','environment-configuration-publisher-kafka',id,'ACTIVE',NOW(),NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='environment-configuration-publisher-kafka';

INSERT INTO schema_definitions (version,identifier,scope_code,code,name,lifecycle_code,created_at,updated_at)
VALUES (0,UUID(),'PLATFORM','environment-configuration-publisher-app-config','environment-configuration-publisher-app-config','ACTIVE',NOW(),NOW());
INSERT INTO schema_versions (identifier,schema_id,schema_version,version_name,definition,status,created_at)
SELECT UUID(),id,1,'environment-configuration-publisher-app-config v1','{"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object","additionalProperties":false,"required":["publisherIdentifier","sortOrder","parameters"],"properties":{"publisherIdentifier":{"type":"string","format":"uuid","pattern":"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"},"sortOrder":{"type":"integer","minimum":1,"maximum":2147483647},"parameters":{"type":"object","additionalProperties":true}}}','PUBLISHED',NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='environment-configuration-publisher-app-config';
INSERT INTO schema_binding (version,identifier,resource_type,resource_code,schema_id,lifecycle_code,created_at,updated_at)
SELECT 0,UUID(),'PLATFORM','environment-configuration-publisher-app-config',id,'ACTIVE',NOW(),NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='environment-configuration-publisher-app-config';
