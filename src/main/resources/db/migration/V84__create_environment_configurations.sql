-- Independent environment settings; existing registration settings and dynamic parameters are unchanged.

CREATE TABLE workspace_environment_configurations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL,
    workspace_id BIGINT NOT NULL,
    environment_id BIGINT NOT NULL,
    lifecycle_code VARCHAR(50) NOT NULL,
    settings JSON NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_workspace_env_config PRIMARY KEY (id),
    CONSTRAINT uk_workspace_env_config_identifier UNIQUE (identifier),
    CONSTRAINT uk_workspace_env_config_owner_environment UNIQUE (workspace_id, environment_id),
    CONSTRAINT fk_workspace_env_config_owner FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
    CONSTRAINT fk_workspace_env_config_environment FOREIGN KEY (environment_id) REFERENCES environments(id),
    CONSTRAINT fk_workspace_env_config_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code)
);
CREATE INDEX idx_workspace_env_config_lifecycle ON workspace_environment_configurations (workspace_id, lifecycle_code, id);

CREATE TABLE workspace_environment_configuration_publishers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    configuration_id BIGINT NOT NULL,
    publisher_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    parameters JSON NOT NULL,
    CONSTRAINT pk_workspace_env_config_publisher PRIMARY KEY (id),
    CONSTRAINT uk_workspace_env_config_publisher UNIQUE (configuration_id, publisher_id),
    CONSTRAINT uk_workspace_env_config_order UNIQUE (configuration_id, sort_order),
    CONSTRAINT fk_workspace_env_config_publisher_owner FOREIGN KEY (configuration_id) REFERENCES workspace_environment_configurations(id),
    CONSTRAINT fk_workspace_env_config_publisher_reference FOREIGN KEY (publisher_id) REFERENCES publishers(id),
    CONSTRAINT ck_workspace_env_config_order CHECK (sort_order > 0)
);

CREATE TABLE application_environment_configurations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL,
    application_id BIGINT NOT NULL,
    environment_id BIGINT NOT NULL,
    lifecycle_code VARCHAR(50) NOT NULL,
    settings JSON NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_application_env_config PRIMARY KEY (id),
    CONSTRAINT uk_application_env_config_identifier UNIQUE (identifier),
    CONSTRAINT uk_application_env_config_owner_environment UNIQUE (application_id, environment_id),
    CONSTRAINT fk_application_env_config_owner FOREIGN KEY (application_id) REFERENCES applications(id),
    CONSTRAINT fk_application_env_config_environment FOREIGN KEY (environment_id) REFERENCES environments(id),
    CONSTRAINT fk_application_env_config_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code)
);
CREATE INDEX idx_application_env_config_lifecycle ON application_environment_configurations (application_id, lifecycle_code, id);

CREATE TABLE application_environment_configuration_publishers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    configuration_id BIGINT NOT NULL,
    publisher_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    parameters JSON NOT NULL,
    CONSTRAINT pk_application_env_config_publisher PRIMARY KEY (id),
    CONSTRAINT uk_application_env_config_publisher UNIQUE (configuration_id, publisher_id),
    CONSTRAINT uk_application_env_config_order UNIQUE (configuration_id, sort_order),
    CONSTRAINT fk_application_env_config_publisher_owner FOREIGN KEY (configuration_id) REFERENCES application_environment_configurations(id),
    CONSTRAINT fk_application_env_config_publisher_reference FOREIGN KEY (publisher_id) REFERENCES publishers(id),
    CONSTRAINT ck_application_env_config_order CHECK (sort_order > 0)
);
