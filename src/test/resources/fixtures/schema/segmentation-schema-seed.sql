UPDATE schema_definitions SET code='category-resource', name='Category request' WHERE scope_code='PLATFORM' AND code='segment-category-resource';
UPDATE schema_configuration SET resource_type='SEGMENTATION', resource_code=CASE WHEN resource_code='segment-category' THEN 'category' ELSE resource_code END WHERE resource_type='PLATFORM' AND resource_code IN ('segment','segment-category');
INSERT INTO schema_versions(identifier,schema_id,schema_version,version_name,definition,status,created_at) SELECT UUID(),sd.id,v.latest+1,'Category Resource v2','{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Segment Category request",
    "type": "object",
    "additionalProperties": false,
    "required": [
        "code",
        "name"
    ],
    "properties": {
        "version": {
            "type": [
                "integer",
                "null"
            ],
            "minimum": 0,
            "maximum": 9223372036854775807
        },
        "code": {
            "type": "string",
            "maxLength": 100,
            "pattern": "^[a-z0-9]+(?:-[a-z0-9]+)*$"
        },
        "name": {
            "type": "string",
            "minLength": 1,
            "maxLength": 150,
            "pattern": "\\\\S"
        }
    }
}
','PUBLISHED',CURRENT_TIMESTAMP FROM schema_definitions sd JOIN (SELECT schema_id,MAX(schema_version) latest FROM schema_versions GROUP BY schema_id) v ON v.schema_id=sd.id WHERE sd.scope_code='PLATFORM' AND sd.code='category-resource';
