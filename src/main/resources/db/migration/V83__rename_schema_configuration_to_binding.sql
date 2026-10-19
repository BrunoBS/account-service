-- Rename the schema-to-resource association in place; all rows and identifiers are preserved.
RENAME TABLE schema_configuration TO schema_binding;

ALTER TABLE schema_binding
    RENAME INDEX uk_schema_configuration_identifier TO uk_schema_binding_identifier,
    RENAME INDEX uk_schema_configuration_resource TO uk_schema_binding_resource;

ALTER TABLE schema_binding
    DROP FOREIGN KEY fk_schema_configuration_schema,
    DROP FOREIGN KEY fk_schema_configuration_lifecycle;

ALTER TABLE schema_binding
    ADD CONSTRAINT fk_schema_binding_schema FOREIGN KEY (schema_id) REFERENCES schema_definitions(id),
    ADD CONSTRAINT fk_schema_binding_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code);

CREATE OR REPLACE VIEW vw_platform_resource_schemas AS
SELECT sb.resource_type,
       sb.resource_code,
       sv.schema_version,
       sv.definition
FROM schema_binding sb
JOIN schema_definitions sd
  ON sd.id = sb.schema_id
JOIN schema_versions sv
  ON sv.schema_id = sd.id
WHERE sb.lifecycle_code = 'ACTIVE'
  AND sd.lifecycle_code = 'ACTIVE'
  AND sv.status = 'PUBLISHED'
  AND sv.schema_version = (
      SELECT MAX(sv2.schema_version)
      FROM schema_versions sv2
      WHERE sv2.schema_id = sd.id
        AND sv2.status = 'PUBLISHED'
  );
