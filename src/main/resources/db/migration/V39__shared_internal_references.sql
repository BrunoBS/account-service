ALTER TABLE shared_contracts
ADD COLUMN owner_workspace_id BIGINT NULL,
ADD COLUMN owner_application_id BIGINT NULL;

UPDATE shared_contracts c
JOIN workspaces w ON w.identifier = c.owner_workspace_identifier
JOIN applications a ON a.identifier = c.owner_application_identifier
AND a.workspace_id = w.id
SET
    c.owner_workspace_id = w.id,
    c.owner_application_id = a.id;

ALTER TABLE shared_contracts
DROP INDEX uk_shared_contracts_scope_name,
DROP INDEX idx_shared_contracts_owner_scope,
MODIFY owner_workspace_id BIGINT NOT NULL,
MODIFY owner_application_id BIGINT NOT NULL,
DROP COLUMN owner_workspace_identifier,
DROP COLUMN owner_application_identifier,
ADD CONSTRAINT uk_shared_contracts_scope_name UNIQUE (owner_workspace_id, owner_application_id, name),
ADD INDEX idx_shared_contracts_owner_scope (
    owner_workspace_id,
    owner_application_id,
    lifecycle_code
),
ADD CONSTRAINT fk_shared_contracts_workspace FOREIGN KEY (owner_workspace_id) REFERENCES workspaces (id),
ADD CONSTRAINT fk_shared_contracts_application FOREIGN KEY (owner_application_id) REFERENCES applications (id);

ALTER TABLE shared_participations
ADD COLUMN participant_workspace_id BIGINT NULL,
ADD COLUMN participant_application_id BIGINT NULL;

UPDATE shared_participations p
JOIN workspaces w ON w.identifier = p.participant_workspace_identifier
JOIN applications a ON a.identifier = p.participant_application_identifier
AND a.workspace_id = w.id
SET
    p.participant_workspace_id = w.id,
    p.participant_application_id = a.id;

ALTER TABLE shared_participations
DROP INDEX uk_shared_participations_contract_application,
DROP INDEX idx_shared_participations_participant,
MODIFY participant_workspace_id BIGINT NOT NULL,
MODIFY participant_application_id BIGINT NOT NULL,
DROP COLUMN participant_workspace_identifier,
DROP COLUMN participant_application_identifier,
ADD CONSTRAINT uk_shared_participations_contract_application UNIQUE (contract_id, participant_application_id),
ADD INDEX idx_shared_participations_participant (
    participant_workspace_id,
    participant_application_id,
    status_code
),
ADD CONSTRAINT fk_shared_participations_workspace FOREIGN KEY (participant_workspace_id) REFERENCES workspaces (id),
ADD CONSTRAINT fk_shared_participations_application FOREIGN KEY (participant_application_id) REFERENCES applications (id);

ALTER TABLE shared_environment_mappings
ADD COLUMN source_environment_id BIGINT NULL,
ADD COLUMN destination_environment_id BIGINT NULL;

UPDATE shared_environment_mappings m
JOIN environments source ON source.identifier = m.source_environment_identifier
JOIN environments destination ON destination.identifier = m.destination_environment_identifier
SET
    m.source_environment_id = source.id,
    m.destination_environment_id = destination.id;

ALTER TABLE shared_environment_mappings
DROP INDEX uk_shared_mapping_destination,
MODIFY source_environment_id BIGINT NOT NULL,
MODIFY destination_environment_id BIGINT NOT NULL,
DROP COLUMN source_environment_identifier,
DROP COLUMN destination_environment_identifier,
ADD CONSTRAINT uk_shared_mapping_destination UNIQUE (participation_id, destination_environment_id),
ADD CONSTRAINT fk_shared_mapping_source_environment FOREIGN KEY (source_environment_id) REFERENCES environments (id),
ADD CONSTRAINT fk_shared_mapping_destination_environment FOREIGN KEY (destination_environment_id) REFERENCES environments (id);
