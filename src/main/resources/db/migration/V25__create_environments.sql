CREATE TABLE environment_types (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL UNIQUE,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(250) NOT NULL,
    root_allowed BOOLEAN NOT NULL,
    workspace_required BOOLEAN NOT NULL,
    lifecycle_code VARCHAR(50) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE TABLE environment_type_compatibilities (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    identifier VARCHAR(36) NOT NULL UNIQUE,
    parent_type_id BIGINT NOT NULL,
    child_type_id BIGINT NOT NULL,
    lifecycle_code VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_environment_compatibility UNIQUE (parent_type_id, child_type_id),
    CONSTRAINT fk_environment_compatibility_parent FOREIGN KEY (parent_type_id) REFERENCES environment_types (id),
    CONSTRAINT fk_environment_compatibility_child FOREIGN KEY (child_type_id) REFERENCES environment_types (id)
);

CREATE TABLE environments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL UNIQUE,
    workspace_id BIGINT NULL,
    environment_type_id BIGINT NOT NULL,
    parent_environment_id BIGINT NULL,
    name VARCHAR(100) NOT NULL,
    name_workspace_scope BIGINT GENERATED ALWAYS AS (COALESCE(workspace_id, 0)) STORED,
    name_parent_scope BIGINT GENERATED ALWAYS AS (COALESCE(parent_environment_id, 0)) STORED,
    description VARCHAR(250) NOT NULL,
    authorization_type_code VARCHAR(50) NOT NULL,
    authorizer_group VARCHAR(255) NULL,
    settings TEXT NOT NULL,
    sort_order INT NOT NULL,
    lifecycle_code VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_environments_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces (id),
    CONSTRAINT fk_environments_authorization FOREIGN KEY (authorization_type_code) REFERENCES type_authorizations (code),
    CONSTRAINT fk_environments_type FOREIGN KEY (environment_type_id) REFERENCES environment_types (id),
    CONSTRAINT fk_environments_parent FOREIGN KEY (parent_environment_id) REFERENCES environments (id),
    CONSTRAINT fk_environments_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle (code),
    CONSTRAINT uk_environments_sibling_name UNIQUE (name_workspace_scope, name_parent_scope, name),
    CONSTRAINT ck_environments_sort CHECK (sort_order >= 1)
);

CREATE INDEX idx_environments_scope_lifecycle ON environments (
    workspace_id,
    lifecycle_code,
    parent_environment_id,
    sort_order
);

CREATE INDEX idx_environments_parent ON environments (parent_environment_id, lifecycle_code, sort_order);

INSERT INTO
    environment_types (
        identifier,
        code,
        name,
        description,
        root_allowed,
        workspace_required,
        lifecycle_code,
        display_order,
        created_at,
        updated_at
    )
VALUES
    (
        UUID(),
        'DEFAULT',
        'Default',
        'Global environment',
        true,
        false,
        'ACTIVE',
        1,
        NOW(6),
        NOW(6)
    ),
    (
        UUID(),
        'CUSTOM',
        'Custom',
        'Workspace environment',
        true,
        true,
        'ACTIVE',
        2,
        NOW(6),
        NOW(6)
    ),
    (
        UUID(),
        'SHARD',
        'Shard',
        'Workspace shard',
        false,
        true,
        'ACTIVE',
        3,
        NOW(6),
        NOW(6)
    ),
    (
        UUID(),
        'CELL',
        'Cell',
        'Workspace cell',
        false,
        true,
        'ACTIVE',
        4,
        NOW(6),
        NOW(6)
    );

INSERT INTO
    environment_type_compatibilities (
        identifier,
        parent_type_id,
        child_type_id,
        lifecycle_code,
        created_at,
        updated_at
    )
SELECT
    UUID(),
    p.id,
    c.id,
    'ACTIVE',
    NOW(6),
    NOW(6)
FROM
    environment_types p
    JOIN environment_types c
WHERE
    (
        p.code = 'DEFAULT'
        AND c.code = 'SHARD'
    )
    OR (
        p.code = 'CUSTOM'
        AND c.code = 'SHARD'
    )
    OR (
        p.code = 'SHARD'
        AND c.code = 'CELL'
    );
