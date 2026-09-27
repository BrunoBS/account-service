CREATE TABLE environments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL,
    workspace_id BIGINT NULL,
    name VARCHAR(100) NOT NULL,
    default_name VARCHAR(100) GENERATED ALWAYS AS (CASE WHEN workspace_id IS NULL THEN name ELSE NULL END) STORED,
    description VARCHAR(250) NOT NULL,
    authorization_type_code VARCHAR(50) NOT NULL,
    environment_type_code VARCHAR(50) NOT NULL,
    authorizer_group VARCHAR(255) NULL,
    settings TEXT NOT NULL,
    sort_order INT NOT NULL,
    lifecycle_code VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_environments PRIMARY KEY (id),
    CONSTRAINT uk_environments_identifier UNIQUE (identifier),
    CONSTRAINT fk_environments_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
    CONSTRAINT fk_environments_authorization FOREIGN KEY (authorization_type_code) REFERENCES type_authorizations(code),
    CONSTRAINT fk_environments_type FOREIGN KEY (environment_type_code) REFERENCES type_environments(code),
    CONSTRAINT fk_environments_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code),
    CONSTRAINT ck_environments_scope CHECK ((environment_type_code = 'DEFAULT' AND workspace_id IS NULL) OR
                                           (environment_type_code = 'CUSTOM' AND workspace_id IS NOT NULL)),
    CONSTRAINT ck_environments_sort CHECK (sort_order >= 1)
);

-- MySQL UNIQUE permits multiple NULLs, so default names need a separate unique key.
CREATE UNIQUE INDEX uk_environments_scope_name ON environments
    (workspace_id, name);
CREATE UNIQUE INDEX uk_environments_default_name ON environments (default_name);
CREATE INDEX idx_environments_scope_lifecycle ON environments
    (environment_type_code, workspace_id, lifecycle_code, sort_order);
