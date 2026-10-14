ALTER TABLE environment_types
    ADD COLUMN allows_children BOOLEAN NOT NULL DEFAULT FALSE AFTER root_allowed;

UPDATE environment_types
SET allows_children = EXISTS (
    SELECT 1
    FROM environment_type_compatibilities compatibility
    WHERE compatibility.parent_type_id = environment_types.id
      AND compatibility.lifecycle_code = 'ACTIVE'
) OR EXISTS (
    SELECT 1
    FROM environments parent_environment
    JOIN environments child_environment
      ON child_environment.parent_environment_id = parent_environment.id
    WHERE parent_environment.environment_type_id = environment_types.id
);

INSERT INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), sd.id, 2, 'Environment Type Resource v2',
    '{
      "$schema":"https://json-schema.org/draft/2020-12/schema",
      "title":"Environment type request",
      "type":"object",
      "additionalProperties":false,
      "required":["code","name","description","rootAllowed","allowsChildren","workspaceRequired","displayOrder"],
      "properties":{
        "version":{"type":["integer","null"],"minimum":0,"maximum":9223372036854775807},
        "code":{"type":"string","enum":["DEFAULT","CUSTOM","SHARD","CELL"],"maxLength":50},
        "name":{"type":"string","minLength":1,"maxLength":100,"pattern":"\\\\S"},
        "description":{"type":"string","minLength":1,"maxLength":250,"pattern":"\\\\S"},
        "rootAllowed":{"type":"boolean"},
        "allowsChildren":{"type":"boolean"},
        "workspaceRequired":{"type":"boolean"},
        "displayOrder":{"type":"integer","minimum":0,"maximum":2147483647}
      }
    }',
    'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions sd
WHERE sd.scope_code = 'PLATFORM' AND sd.code = 'environment-type-resource';
