INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), sd.id, 2, 'Environment Resource v2',
    '{
      "$schema":"https://json-schema.org/draft/2020-12/schema",
      "title":"Environment request",
      "type":"object",
      "additionalProperties":false,
      "required":["name","description","authorizationType","settings","environmentTypeCode"],
      "properties":{
        "version":{"type":["integer","null"],"minimum":0,"maximum":9223372036854775807},
        "name":{"type":"string","minLength":3,"maxLength":50,"pattern":"\\\\S"},
        "description":{"type":"string","minLength":3,"maxLength":200,"pattern":"\\\\S"},
        "authorizationType":{"type":"string","enum":["DEV","TST","ADM"],"maxLength":50},
        "sortOrder":{"type":["integer","null"],"minimum":1,"maximum":2147483647},
        "authorizerGroup":{"type":["string","null"],"maxLength":253},
        "settings":{"type":"object","description":"Environment-specific settings; properties are configurable by the platform.","additionalProperties":true},
        "environmentTypeCode":{"type":"string","enum":["DEFAULT","CUSTOM","SHARD","CELL"],"maxLength":50},
        "parentIdentifier":{"type":["string","null"],"minLength":1,"maxLength":36,"format":"uuid"}
      }
    }',
    'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions sd
WHERE sd.scope_code = 'PLATFORM' AND sd.code = 'environment-resource';

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), sd.id, 3, 'Environment Type Resource v3',
    '{
      "$schema":"https://json-schema.org/draft/2020-12/schema",
      "title":"Environment type request",
      "type":"object",
      "additionalProperties":false,
      "required":["code","name","description","rootAllowed","allowsChildren","workspaceRequired","displayOrder"],
      "properties":{
        "version":{"type":["integer","null"],"minimum":0,"maximum":9223372036854775807},
        "code":{"type":"string","enum":["DEFAULT","CUSTOM","SHARD","CELL"],"minLength":1,"maxLength":50,"pattern":"\\\\S"},
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
SELECT UUID(), sd.id, 3, 'Environment Type Compatibility Resource v3',
    '{"$schema":"https://json-schema.org/draft/2020-12/schema","title":"Environment type compatibility request","type":"object","additionalProperties":false,"required":["parentTypeCode","childTypeCode"],"properties":{"parentTypeCode":{"type":"string","enum":["DEFAULT","CUSTOM","SHARD","CELL"],"minLength":1,"maxLength":50,"pattern":"\\\\S"},"childTypeCode":{"type":"string","enum":["DEFAULT","CUSTOM","SHARD","CELL"],"minLength":1,"maxLength":50,"pattern":"\\\\S"}}}',
    'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions sd
WHERE sd.scope_code = 'PLATFORM' AND sd.code = 'environment-type-compatibility-resource';
