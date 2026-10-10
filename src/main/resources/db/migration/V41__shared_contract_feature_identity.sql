ALTER TABLE shared_contracts
DROP INDEX uk_shared_contracts_scope_name,
DROP COLUMN name,
ADD CONSTRAINT uk_shared_contracts_scope_feature UNIQUE (
    owner_workspace_id,
    owner_application_id,
    feature_id
);
