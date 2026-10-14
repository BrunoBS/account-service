-- Public read contract consumed by platform-schema-validation.
-- Ownership remains in workspace-service; consumers know only this VIEW.
CREATE OR REPLACE VIEW vw_platform_resource_schemas AS
SELECT sc.resource_type,
       sc.resource_code,
       sv.schema_version,
       sv.definition
FROM schema_configuration sc
JOIN schema_definitions sd
  ON sd.id = sc.schema_id
JOIN schema_versions sv
  ON sv.schema_id = sd.id
WHERE sc.lifecycle_code = 'ACTIVE'
  AND sd.lifecycle_code = 'ACTIVE'
  AND sv.status = 'PUBLISHED'
  AND sv.schema_version = (
      SELECT MAX(sv2.schema_version)
      FROM schema_versions sv2
      WHERE sv2.schema_id = sd.id
        AND sv2.status = 'PUBLISHED'
  );
