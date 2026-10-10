CREATE TABLE workspace_tag (
    id BIGINT NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT NOT NULL,
    name VARCHAR(150) COLLATE utf8mb4_unicode_ci NOT NULL,
    origin_type VARCHAR(20) COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_workspace_tag UNIQUE (workspace_id, name),
    CONSTRAINT fk_workspace_tag_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces (id),
    CONSTRAINT ck_workspace_tag_origin_type CHECK (origin_type IN ('MANUAL', 'SYSTEM')),
    KEY idx_workspace_tag_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE application_tag (
    id BIGINT NOT NULL AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    name VARCHAR(150) COLLATE utf8mb4_unicode_ci NOT NULL,
    origin_type VARCHAR(20) COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_application_tag UNIQUE (application_id, name),
    CONSTRAINT fk_application_tag_application FOREIGN KEY (application_id) REFERENCES applications (id),
    CONSTRAINT ck_application_tag_origin_type CHECK (origin_type IN ('MANUAL', 'SYSTEM')),
    KEY idx_application_tag_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
