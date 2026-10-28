-- Evolve only the request schema in existing schema_versions; no tables or columns are added.
-- Preserve previously published definitions and the V1 checksum.
INSERT INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), definition.id, COALESCE(history.latest_version, 0) + 1,
       'Scoped parameter publication',
       '{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "Parameter key publication",
  "type": "object",
  "additionalProperties": false,
  "required": [
    "microserviceIdentifiers"
  ],
  "properties": {
    "identifiers": {
      "type": [
        "array",
        "null"
      ],
      "minItems": 0,
      "uniqueItems": true,
      "items": {
        "type": "string",
        "format": "uuid",
        "maxLength": 36
      }
    },
    "microserviceIdentifiers": {
      "type": "array",
      "minItems": 1,
      "uniqueItems": true,
      "items": {
        "type": "string",
        "format": "uuid",
        "maxLength": 36
      }
    },
    "all": {
      "type": "boolean",
      "default": false
    }
  },
  "allOf": [
    {
      "if": {
        "properties": {
          "all": {
            "const": true
          }
        },
        "required": [
          "all"
        ]
      },
      "then": {},
      "else": {
        "required": [
          "identifiers"
        ],
        "properties": {
          "identifiers": {
            "minItems": 1,
            "type": "array"
          }
        }
      }
    }
  ]
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions definition
LEFT JOIN (SELECT schema_id, MAX(schema_version) AS latest_version FROM schema_versions GROUP BY schema_id) history
    ON history.schema_id = definition.id
WHERE definition.scope_code = 'PLATFORM' AND definition.code = 'dynamic-parameter-publication';
