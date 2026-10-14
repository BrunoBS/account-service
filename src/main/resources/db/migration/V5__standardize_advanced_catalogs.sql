ALTER TABLE type_features
DROP
FOREIGN KEY fk_type_features_scope,
DROP INDEX uk_type_features_scope_name,
DROP
COLUMN type_feature_scopes_id,
    DROP
COLUMN available;

ALTER TABLE type_schemas
DROP
FOREIGN KEY fk_type_schemas_scope,
DROP INDEX uk_type_schemas_scope_name,
DROP
COLUMN type_schema_scopes_id;

ALTER TABLE type_onboardings
DROP
COLUMN orientation;
