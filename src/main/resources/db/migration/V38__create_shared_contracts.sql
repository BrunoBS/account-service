CREATE TABLE shared_contracts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL,
    owner_workspace_identifier VARCHAR(36) NOT NULL,
    owner_application_identifier VARCHAR(36) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    lifecycle_code VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_shared_contracts_identifier UNIQUE (identifier),
    CONSTRAINT uk_shared_contracts_scope_name UNIQUE (
        owner_workspace_identifier,
        owner_application_identifier,
        name
    ),
    INDEX idx_shared_contracts_owner_scope (
        owner_workspace_identifier,
        owner_application_identifier,
        lifecycle_code
    )
);

CREATE TABLE shared_participations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL,
    contract_id BIGINT NOT NULL,
    participant_workspace_identifier VARCHAR(36) NOT NULL,
    participant_application_identifier VARCHAR(36) NOT NULL,
    status_code VARCHAR(30) NOT NULL,
    publication_mode_code VARCHAR(30) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_shared_participations_identifier UNIQUE (identifier),
    CONSTRAINT uk_shared_participations_contract_application UNIQUE (contract_id, participant_application_identifier),
    CONSTRAINT fk_shared_participations_contract FOREIGN KEY (contract_id) REFERENCES shared_contracts (id) ON DELETE CASCADE,
    CONSTRAINT fk_shared_participations_status FOREIGN KEY (status_code) REFERENCES type_sharing_statuses (code),
    CONSTRAINT fk_shared_participations_publication_mode FOREIGN KEY (publication_mode_code) REFERENCES type_publication_modes (code),
    INDEX idx_shared_participations_participant (
        participant_workspace_identifier,
        participant_application_identifier,
        status_code
    )
);

CREATE TABLE shared_environment_mappings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    participation_id BIGINT NOT NULL,
    source_environment_identifier VARCHAR(36) NOT NULL,
    destination_environment_identifier VARCHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_shared_mapping_destination UNIQUE (
        participation_id,
        destination_environment_identifier
    ),
    CONSTRAINT fk_shared_mapping_participation FOREIGN KEY (participation_id) REFERENCES shared_participations (id) ON DELETE CASCADE
);
