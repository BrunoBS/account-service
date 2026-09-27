-- Preserve platform bindings before removing the legacy schema type directory.
CREATE TABLE schema_configuration (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL,
    resource_type VARCHAR(30) NOT NULL,
    resource_code VARCHAR(50) NOT NULL,
    schema_id BIGINT NOT NULL,
    lifecycle_code VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_schema_configuration PRIMARY KEY (id),
    CONSTRAINT uk_schema_configuration_identifier UNIQUE (identifier),
    CONSTRAINT uk_schema_configuration_resource UNIQUE (resource_type, resource_code),
    CONSTRAINT fk_schema_configuration_schema FOREIGN KEY (schema_id) REFERENCES schema_definitions(id),
    CONSTRAINT fk_schema_configuration_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code),
    CONSTRAINT ck_schema_configuration_resource_type CHECK
        (resource_type IN ('WORKSPACE', 'APPLICATION', 'ENVIRONMENT', 'PUBLISHER',
                           'FEATURE', 'MICROSERVICE', 'CATALOG'))
);

INSERT INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(),
       CASE
           WHEN sd.schema_type_code IN ('WORKSPACE', 'APPLICATION', 'ENVIRONMENT', 'PUBLISHER', 'FEATURE', 'MICROSERVICE')
               THEN sd.schema_type_code
           WHEN LEFT(sd.schema_type_code, 10) = 'PUBLISHER_' THEN 'PUBLISHER'
           WHEN LEFT(sd.schema_type_code, 8) = 'FEATURE_' THEN 'FEATURE'
           WHEN RIGHT(sd.schema_type_code, 5) = '_TYPE' THEN 'CATALOG'
       END,
       CASE
           WHEN LEFT(sd.schema_type_code, 10) = 'PUBLISHER_'
               THEN SUBSTRING(sd.schema_type_code, 11)
           WHEN LEFT(sd.schema_type_code, 8) = 'FEATURE_'
               THEN LOWER(REPLACE(SUBSTRING(sd.schema_type_code, 9), '_', '-'))
           WHEN RIGHT(sd.schema_type_code, 5) = '_TYPE'
               THEN LOWER(REPLACE(sd.schema_type_code, '_', '-'))
           WHEN sd.schema_type_code = 'ENVIRONMENT' THEN 'workspace'
           WHEN sd.schema_type_code = 'PUBLISHER' THEN UPPER(sd.code)
           WHEN sd.schema_type_code IN ('WORKSPACE', 'APPLICATION', 'FEATURE', 'MICROSERVICE')
               THEN LOWER(REPLACE(sd.code, '_', '-'))
       END,
       sd.id,
       CASE WHEN st.lifecycle_code = 'ACTIVE' THEN sd.lifecycle_code ELSE 'INACTIVE' END,
       sd.created_at, sd.updated_at
FROM schema_definitions sd
JOIN schema_types st ON st.code = sd.schema_type_code
WHERE sd.scope_code = 'PLATFORM'
  AND sd.schema_type_code <> 'DEFAULT'
  AND sd.lifecycle_code <> 'QUARANTINED'
  AND (sd.schema_type_code IN ('WORKSPACE', 'APPLICATION', 'ENVIRONMENT', 'PUBLISHER', 'FEATURE', 'MICROSERVICE')
       OR LEFT(sd.schema_type_code, 10) = 'PUBLISHER_'
       OR LEFT(sd.schema_type_code, 8) = 'FEATURE_'
       OR RIGHT(sd.schema_type_code, 5) = '_TYPE');

-- The old unique key included schema_type_code; preserve colliding schema rows
-- by giving only the conflicting codes a deterministic, valid suffix.
UPDATE schema_definitions sd
JOIN (
    SELECT scope_code, owner_key, code
    FROM schema_definitions
    GROUP BY scope_code, owner_key, code
    HAVING COUNT(*) > 1
) collisions ON collisions.scope_code = sd.scope_code
            AND collisions.owner_key = sd.owner_key
            AND collisions.code = sd.code
SET sd.code = CONCAT(LEFT(sd.code, 30), '-', LEFT(MD5(sd.schema_type_code), 8));

DELETE sv FROM schema_versions sv
JOIN schema_definitions sd ON sd.id = sv.schema_id
WHERE sd.scope_code = 'PLATFORM' AND sd.schema_type_code = 'DEFAULT';
DELETE FROM schema_definitions WHERE scope_code = 'PLATFORM' AND schema_type_code = 'DEFAULT';

ALTER TABLE schema_definitions DROP FOREIGN KEY fk_schema_definitions_type;
ALTER TABLE schema_definitions DROP INDEX uk_schema_definitions_scope_owner_type_code;
ALTER TABLE schema_definitions DROP INDEX uk_schema_definitions_platform_type;
ALTER TABLE schema_definitions DROP INDEX idx_schema_definitions_type_scope_owner;
ALTER TABLE schema_definitions DROP COLUMN platform_schema_type_code;
ALTER TABLE schema_definitions DROP COLUMN schema_type_code;
ALTER TABLE schema_definitions
    ADD CONSTRAINT uk_schema_definitions_scope_owner_code UNIQUE (scope_code, owner_key, code);

DROP TABLE schema_type_scopes;
DROP TABLE schema_types;
