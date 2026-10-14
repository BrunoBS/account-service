-- Publish one complete request schema for each environment resource.
INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES
    (0, UUID(), 'PLATFORM', 'environment-resource', 'Environment Resource', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (0, UUID(), 'PLATFORM', 'environment-type-resource', 'Environment Type Resource', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (0, UUID(), 'PLATFORM', 'environment-type-compatibility-resource', 'Environment Type Compatibility Resource', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), sd.id, 1, 'Environment Resource v1',
    '{
      "$schema":"https://json-schema.org/draft/2020-12/schema",
      "title":"Environment request",
      "type":"object",
      "additionalProperties":false,
      "required":["name","description","authorizationType","settings"],
      "properties":{
        "version":{"type":["integer","null"],"minimum":0,"maximum":9223372036854775807},
        "name":{"type":"string","minLength":3,"maxLength":50,"pattern":"\\\\S"},
        "description":{"type":"string","minLength":3,"maxLength":250,"pattern":"\\\\S"},
        "authorizationType":{"type":"string","enum":["DEV","TST","ADM"],"maxLength":50},
        "sortOrder":{"type":["integer","null"],"minimum":1,"maximum":2147483647},
        "authorizerGroup":{"type":["string","null"],"maxLength":253},
        "settings":{"type":"object","description":"Environment-specific settings; properties are configurable by the platform.","additionalProperties":true},
        "environmentTypeCode":{"type":["string","null"],"minLength":1,"maxLength":50,"pattern":"\\\\S"},
        "parentIdentifier":{"type":["string","null"],"minLength":1,"maxLength":36,"pattern":"\\\\S"}
      }
    }',
    'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions sd
WHERE sd.scope_code = 'PLATFORM' AND sd.code = 'environment-resource';

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), sd.id, 1, 'Environment Type Resource v1',
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

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), sd.id, 1, 'Environment Type Compatibility Resource v1',
    '{
      "$schema":"https://json-schema.org/draft/2020-12/schema",
      "title":"Environment type compatibility request",
      "type":"object",
      "additionalProperties":false,
      "required":["parentTypeCode","childTypeCode"],
      "properties":{
        "parentTypeCode":{"type":"string","minLength":1,"maxLength":50,"pattern":"\\\\S"},
        "childTypeCode":{"type":"string","minLength":1,"maxLength":50,"pattern":"\\\\S"}
      }
    }',
    'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions sd
WHERE sd.scope_code = 'PLATFORM' AND sd.code = 'environment-type-compatibility-resource';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'ENVIRONMENT', bindings.resource_code, sd.id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions sd
CROSS JOIN (
    SELECT 'environment-resource' AS definition_code, 'environment' AS resource_code UNION ALL
    SELECT 'environment-type-resource', 'environment-type' UNION ALL
    SELECT 'environment-type-compatibility-resource', 'environment-type-compatibility'
) bindings
WHERE sd.scope_code = 'PLATFORM' AND sd.code = bindings.definition_code;
