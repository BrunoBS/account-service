ALTER TABLE publishers
    MODIFY COLUMN code VARCHAR(50) NOT NULL,
    DROP CHECK ck_publishers_code;

INSERT INTO schema_versions (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT
    UUID(),
    schema_id,
    2,
    'Publisher Resource v2',
    JSON_SET(
        definition,
        '$.properties.code',
        JSON_OBJECT('type', 'string', 'enum', JSON_ARRAY('KAFKA', 'HTTP', 'APPCONFIG'), 'maxLength', 50),
        '$.allOf[2].then.properties.settings.properties.timeout.maximum',
        60000,
        '$.allOf[2].then.properties.settings.properties.timeout.description',
        'Timeout in milliseconds.'
    ),
    'PUBLISHED',
    CURRENT_TIMESTAMP
FROM schema_versions
WHERE schema_version = 1
  AND status = 'PUBLISHED'
  AND schema_id = (
      SELECT id FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'publisher-resource'
  );
