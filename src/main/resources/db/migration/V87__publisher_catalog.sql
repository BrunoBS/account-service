-- Generic catalog cutover. Published V1-V86 are unchanged.
-- Legacy identity/metadata remain in their columns, nullable and outside CatalogDTO/JPA.
DELIMITER $$
CREATE PROCEDURE validate_publisher_catalog_v87()
BEGIN
 IF EXISTS (SELECT 1 FROM publishers WHERE code NOT IN ('WEB_SOCKET','KAFKA','APP_CONFIG')) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unknown publisher code; reconcile catalog before V87';
 END IF;
 IF EXISTS (SELECT 1 FROM publishers WHERE JSON_TYPE(settings)<>'OBJECT'
   OR (JSON_CONTAINS_PATH(settings,'one','$.scope') AND JSON_UNQUOTE(JSON_EXTRACT(settings,'$.scope'))<>publisher_scope)) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Publisher settings/scope conflict; reconcile before V87';
 END IF;
END$$
DELIMITER ;
CALL validate_publisher_catalog_v87();
DROP PROCEDURE validate_publisher_catalog_v87;
ALTER TABLE publishers ADD COLUMN label VARCHAR(100) NULL, ADD COLUMN sort_order INT NULL, ADD COLUMN is_active BOOLEAN NULL;
UPDATE publishers SET label=name,sort_order=id,is_active=(lifecycle_code='ACTIVE'),settings=JSON_SET(settings,'$.scope',publisher_scope);
ALTER TABLE workspace_environment_configuration_publishers ADD COLUMN publisher_code VARCHAR(50) NULL;
UPDATE workspace_environment_configuration_publishers a JOIN publishers p ON p.id=a.publisher_id SET a.publisher_code=p.code;
ALTER TABLE workspace_environment_configuration_publishers
 DROP FOREIGN KEY fk_workspace_env_config_publisher_reference,
 DROP INDEX uk_workspace_env_config_publisher,
 MODIFY publisher_id BIGINT NULL,
 MODIFY publisher_code VARCHAR(50) NOT NULL,
 ADD CONSTRAINT uk_workspace_env_config_publisher UNIQUE(configuration_id,publisher_code),
 ADD CONSTRAINT fk_workspace_env_config_publisher_catalog FOREIGN KEY(publisher_code) REFERENCES publishers(code);
ALTER TABLE application_environment_configuration_publishers ADD COLUMN publisher_code VARCHAR(50) NULL;
UPDATE application_environment_configuration_publishers a JOIN publishers p ON p.id=a.publisher_id SET a.publisher_code=p.code;
ALTER TABLE application_environment_configuration_publishers
 DROP FOREIGN KEY fk_application_env_config_publisher_reference,
 DROP INDEX uk_application_env_config_publisher,
 MODIFY publisher_id BIGINT NULL,
 MODIFY publisher_code VARCHAR(50) NOT NULL,
 ADD CONSTRAINT uk_application_env_config_publisher UNIQUE(configuration_id,publisher_code),
 ADD CONSTRAINT fk_application_env_config_publisher_catalog FOREIGN KEY(publisher_code) REFERENCES publishers(code);
ALTER TABLE publishers
 DROP FOREIGN KEY fk_publishers_scope,
 ADD UNIQUE KEY uk_publishers_legacy_id(id),
 DROP PRIMARY KEY, ADD PRIMARY KEY(code),
 MODIFY label VARCHAR(100) NOT NULL, MODIFY sort_order INT NOT NULL, MODIFY is_active BOOLEAN NOT NULL,
 MODIFY identifier VARCHAR(36) NULL, MODIFY name VARCHAR(50) NULL, MODIFY publisher_scope VARCHAR(50) NULL,
 MODIFY lifecycle_code VARCHAR(50) NULL, MODIFY description TEXT NULL, MODIFY settings TEXT NOT NULL,
 MODIFY created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 MODIFY updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
INSERT INTO publishers(code,label,description,sort_order,is_active,settings)
SELECT 'WEB_SOCKET','WebSocket','Workspace publisher',1,TRUE,'{"scope":"WORKSPACE"}' WHERE NOT EXISTS(SELECT 1 FROM publishers WHERE code='WEB_SOCKET');
INSERT INTO publishers(code,label,description,sort_order,is_active,settings)
SELECT 'KAFKA','Kafka','Workspace publisher',2,TRUE,'{"scope":"WORKSPACE"}' WHERE NOT EXISTS(SELECT 1 FROM publishers WHERE code='KAFKA');
INSERT INTO publishers(code,label,description,sort_order,is_active,settings)
SELECT 'APP_CONFIG','AppConfig','Application publisher',3,TRUE,'{"scope":"APPLICATION"}' WHERE NOT EXISTS(SELECT 1 FROM publishers WHERE code='APP_CONFIG');
INSERT INTO schema_versions(identifier,schema_id,schema_version,version_name,definition,status,created_at)
SELECT UUID(),d.id,(SELECT COALESCE(MAX(v.schema_version),0)+1 FROM schema_versions v WHERE v.schema_id=d.id),'workspace-environment-configuration catalog v87','{"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object","additionalProperties":false,"required":["settings","publishers"],"properties":{"version":{"type":["integer","null"],"minimum":0,"maximum":9223372036854775807},"settings":{"type":"object","additionalProperties":true},"publishers":{"type":"array","minItems":1,"items":{"type":"object","additionalProperties":false,"required":["publisherCode","sortOrder","parameters"],"properties":{"sortOrder":{"type":"integer","minimum":1,"maximum":2147483647},"parameters":{"type":"object","additionalProperties":true},"publisherCode":{"type":"string","enum":["WEB_SOCKET","KAFKA","APP_CONFIG"]}}}},"environmentIdentifier":{"type":"string","format":"uuid"}},"if":{"required":["version"],"properties":{"version":{"type":"integer"}}},"then":{"not":{"required":["environmentIdentifier"]}},"else":{"required":["environmentIdentifier"]}}','PUBLISHED',NOW()
FROM schema_definitions d WHERE d.scope_code='PLATFORM' AND d.code='workspace-environment-configuration';
INSERT INTO schema_versions(identifier,schema_id,schema_version,version_name,definition,status,created_at)
SELECT UUID(),d.id,(SELECT COALESCE(MAX(v.schema_version),0)+1 FROM schema_versions v WHERE v.schema_id=d.id),'application-environment-configuration catalog v87','{"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object","additionalProperties":false,"required":["settings"],"properties":{"version":{"type":["integer","null"],"minimum":0,"maximum":9223372036854775807},"settings":{"type":"object","additionalProperties":true},"publishers":{"type":"array","minItems":1,"items":{"type":"object","additionalProperties":false,"required":["publisherCode","sortOrder","parameters"],"properties":{"sortOrder":{"type":"integer","minimum":1,"maximum":2147483647},"parameters":{"type":"object","additionalProperties":true},"publisherCode":{"type":"string","enum":["WEB_SOCKET","KAFKA","APP_CONFIG"]}}}},"environmentIdentifier":{"type":"string","format":"uuid"},"copyWorkspacePublishers":{"type":"boolean","default":false}},"allOf":[{"if":{"required":["version"],"properties":{"version":{"type":"integer"}}},"then":{"not":{"required":["environmentIdentifier"]}},"else":{"required":["environmentIdentifier"]}},{"if":{"required":["copyWorkspacePublishers"],"properties":{"copyWorkspacePublishers":{"const":true}}},"then":{"not":{"required":["publishers"]}},"else":{"required":["publishers"]}}]}','PUBLISHED',NOW()
FROM schema_definitions d WHERE d.scope_code='PLATFORM' AND d.code='application-environment-configuration';
INSERT INTO schema_definitions(version,identifier,scope_code,code,name,lifecycle_code,created_at,updated_at)
VALUES (0,UUID(),'PLATFORM','publisher-type-settings','Publisher catalog settings','ACTIVE',NOW(),NOW());
INSERT INTO schema_versions(identifier,schema_id,schema_version,version_name,definition,status,created_at)
SELECT UUID(),id,1,'Publisher catalog settings v1','{"type":"object","additionalProperties":true}','PUBLISHED',NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='publisher-type-settings';
INSERT INTO schema_binding(version,identifier,resource_type,resource_code,schema_id,lifecycle_code,created_at,updated_at)
SELECT 0,UUID(),'CATALOG','publisher-type',id,'ACTIVE',NOW(),NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='publisher-type-settings';
