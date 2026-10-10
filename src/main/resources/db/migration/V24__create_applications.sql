CREATE TABLE applications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL,
    workspace_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    alias VARCHAR(100) NOT NULL,
    acronym VARCHAR(20) NOT NULL,
    application_scope_code VARCHAR(50) NOT NULL,
    authorizer_group VARCHAR(255) NULL,
    settings TEXT NOT NULL,
    lifecycle_code VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_applications PRIMARY KEY (id),
    CONSTRAINT uk_applications_identifier UNIQUE (identifier),
    CONSTRAINT uk_applications_workspace_name UNIQUE (workspace_id, name),
    CONSTRAINT fk_applications_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces (id),
    CONSTRAINT fk_applications_scope FOREIGN KEY (application_scope_code) REFERENCES type_application_scopes (code),
    CONSTRAINT fk_applications_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle (code)
);

CREATE INDEX idx_applications_workspace_lifecycle ON applications (workspace_id, lifecycle_code);
