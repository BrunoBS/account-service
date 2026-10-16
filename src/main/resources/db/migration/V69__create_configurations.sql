CREATE TABLE configurations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL,
    microservice_id BIGINT NOT NULL,
    configuration_key VARCHAR(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    description VARCHAR(200),
    type_code VARCHAR(50) NOT NULL,
    value JSON NOT NULL,
    published_value JSON,
    published_type_code VARCHAR(50),
    status_code VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_configurations PRIMARY KEY (id),
    CONSTRAINT uk_configuration_identifier UNIQUE (identifier),
    CONSTRAINT uk_configuration_microservice_key UNIQUE (microservice_id, configuration_key),
    CONSTRAINT ck_configuration_key CHECK (REGEXP_LIKE(configuration_key, '^[A-Za-z0-9]+([._-][A-Za-z0-9]+)*$', 'c')),
    CONSTRAINT ck_configuration_snapshot CHECK (
        (published_value IS NULL AND published_type_code IS NULL)
        OR (published_value IS NOT NULL AND published_type_code IS NOT NULL)
    ),
    CONSTRAINT ck_configuration_inactive CHECK (
        active = true OR (status_code = 'DRAFT' AND published_value IS NULL AND published_type_code IS NULL)
    ),
    CONSTRAINT ck_configuration_published CHECK (
        status_code <> 'PUBLISHED' OR (active = true AND published_value IS NOT NULL)
    ),
    CONSTRAINT fk_configuration_microservice FOREIGN KEY (microservice_id) REFERENCES platform_microservices(id),
    CONSTRAINT fk_configuration_type FOREIGN KEY (type_code) REFERENCES type_configuration_types(code),
    CONSTRAINT fk_configuration_published_type FOREIGN KEY (published_type_code) REFERENCES type_configuration_types(code),
    CONSTRAINT fk_configuration_status FOREIGN KEY (status_code) REFERENCES type_configuration_statuses(code)
);

CREATE INDEX idx_configuration_active_id ON configurations (active, id);
