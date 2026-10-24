INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), sd.id, 2, 'Environment Type Compatibility Resource v2',
    '{
      "$schema":"https://json-schema.org/draft/2020-12/schema",
      "title":"Environment type compatibility request",
      "type":"object",
      "additionalProperties":false,
      "required":["parentTypeIdentifier","childTypeIdentifier"],
      "properties":{
        "parentTypeIdentifier":{"type":"string","format":"uuid"},
        "childTypeIdentifier":{"type":"string","format":"uuid"}
      }
    }',
    'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions sd
WHERE sd.scope_code = 'PLATFORM' AND sd.code = 'environment-type-compatibility-resource';
