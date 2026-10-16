-- Existing mandatory FK and per-segment unique constraint already prohibit orphans and duplicates.
-- RENAME preserves every record; no cleanup or deletion is performed.
RENAME TABLE segment_category TO category;
ALTER TABLE category
 RENAME INDEX uk_segment_category_identifier TO uk_category_identifier,
 RENAME INDEX uk_segment_category_code TO uk_category_code,
 RENAME INDEX fk_segment_category_lifecycle TO fk_category_lifecycle,
 DROP FOREIGN KEY fk_segment_category_lifecycle,
 DROP FOREIGN KEY fk_segment_category_segment,
 DROP CHECK ck_segment_category_code,
 DROP CHECK ck_segment_category_name,
 DROP CHECK ck_segment_category_lifecycle,
 ADD CONSTRAINT fk_category_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code),
 ADD CONSTRAINT fk_category_segment FOREIGN KEY (segment_id) REFERENCES segment(id),
 ADD CONSTRAINT ck_category_code CHECK (REGEXP_LIKE(code,'^[a-z0-9]+(-[a-z0-9]+)*$','c')),
 ADD CONSTRAINT ck_category_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),
 ADD CONSTRAINT ck_category_lifecycle CHECK (lifecycle_code IN ('ACTIVE','INACTIVE'));
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
