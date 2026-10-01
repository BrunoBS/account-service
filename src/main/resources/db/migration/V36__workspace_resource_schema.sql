-- Golden contract: strict structural validation for Workspace resources.
INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES
    (0, UUID(), 'PLATFORM', 'workspace-resource', 'Workspace Resource', 'ACTIVE',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), sd.id, 1, 'Workspace Resource v1',
       '{
          "type":"object",
          "additionalProperties":false,
          "required":["workspaceType","name","description","requester","acronym","emailGroup","approvers"],
          "properties":{
            "version":{"type":"integer","minimum":0},
            "workspaceType":{"type":"string","minLength":1,"maxLength":50,"pattern":"^[A-Z][A-Z0-9_-]*$"},
            "name":{"type":"string","minLength":3,"maxLength":100,"pattern":"^\\S(?:.*\\S)?$"},
            "description":{"type":"string","minLength":10,"maxLength":500,"pattern":"^\\S(?:.*\\S)?$"},
            "requester":{"type":"string","minLength":5,"maxLength":255,"pattern":"^\\S(?:.*\\S)?$"},
            "acronym":{"type":"string","minLength":1,"maxLength":5,"pattern":"^[A-Z0-9]+$"},
            "authorizerGroup":{"type":"string","minLength":1,"maxLength":255,"pattern":"^\\S(?:.*\\S)?$"},
            "settings":{"type":"object","additionalProperties":true},
            "emailGroup":{"type":"string","minLength":3,"maxLength":320,"pattern":"^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"},
            "approvers":{
              "type":"array",
              "minItems":1,
              "items":{
                "type":"object",
                "additionalProperties":false,
                "required":["functional","email"],
                "properties":{
                  "functional":{"type":"string","minLength":1,"maxLength":255,"pattern":"^\\S(?:.*\\S)?$"},
                  "email":{"type":"string","minLength":3,"maxLength":320,"pattern":"^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"}
                }
              }
            },
            "tags":{
              "type":"array",
              "uniqueItems":true,
              "items":{"type":"string","minLength":1,"maxLength":150,"pattern":"^[a-z0-9]+(-[a-z0-9]+)*$"}
            }
          }
        }',
       'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions sd
WHERE sd.scope_code = 'PLATFORM' AND sd.code = 'workspace-resource';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'WORKSPACE', 'workspace', sd.id, 'ACTIVE',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions sd
WHERE sd.scope_code = 'PLATFORM' AND sd.code = 'workspace-resource';
