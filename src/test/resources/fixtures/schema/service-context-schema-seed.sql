UPDATE schema_definitions SET code='feature-group-resource', name='Feature Group Resource' WHERE scope_code='PLATFORM' AND code='context-resource';
UPDATE schema_configuration SET resource_type='SERVICE_CONTEXT', resource_code=CASE WHEN resource_code='context' THEN 'feature-group' ELSE resource_code END WHERE resource_type='PLATFORM' AND resource_code IN ('microservice','feature','context');
INSERT INTO schema_versions(identifier,schema_id,schema_version,version_name,definition,status,created_at) SELECT UUID(),sd.id,v.latest+1,'Feature Group Resource v3','{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Feature Group request",
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
            "maxLength": 50,
            "pattern": "^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$"
        },
        "name": {
            "type": "string",
            "minLength": 1,
            "maxLength": 100,
            "pattern": "\\\\S"
        },
        "description": {
            "type": [
                "string",
                "null"
            ],
            "maxLength": 500
        }
    }
}
','PUBLISHED',CURRENT_TIMESTAMP FROM schema_definitions sd JOIN (SELECT schema_id,MAX(schema_version) latest FROM schema_versions GROUP BY schema_id) v ON v.schema_id=sd.id WHERE sd.scope_code='PLATFORM' AND sd.code='feature-group-resource';
