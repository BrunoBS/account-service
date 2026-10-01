-- Golden POC: full structural contract for Application.
INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES
    (0, UUID(), 'PLATFORM', 'application-resource', 'Application Resource', 'ACTIVE',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), sd.id, 1, 'Application Resource v1',
       '{
          "type":"object",
          "additionalProperties":false,
          "required":["name","alias","acronym","applicationScope"],
          "properties":{
            "version":{"type":"integer","minimum":0},
            "name":{"type":"string","minLength":3,"maxLength":100},
            "alias":{"type":"string","minLength":1,"maxLength":100},
            "acronym":{"type":"string","minLength":1,"maxLength":20},
            "applicationScope":{"type":"string","enum":["BACKEND","FRONTEND","SHARED"]},
            "authorizerGroup":{"type":["string","null"],"minLength":1,"maxLength":255},
            "settings":{"type":["object","null"],"additionalProperties":true},
            "tags":{
              "type":["array","null"],
              "uniqueItems":true,
              "items":{
                "type":"string",
                "minLength":1,
                "maxLength":150,
                "pattern":"^[a-z0-9]+(-[a-z0-9]+)*$"
              }
            }
          }
        }',
       'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions sd
WHERE sd.scope_code = 'PLATFORM'
  AND sd.code = 'application-resource';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'APPLICATION', 'application', sd.id, 'ACTIVE',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions sd
WHERE sd.scope_code = 'PLATFORM'
  AND sd.code = 'application-resource';
