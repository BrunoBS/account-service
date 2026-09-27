CREATE TABLE publishers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(500) NOT NULL,
    publisher_scope VARCHAR(20) NOT NULL,
    deprecated BOOLEAN NOT NULL DEFAULT FALSE,
    lifecycle_code VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_publishers PRIMARY KEY (id),
    CONSTRAINT uk_publishers_identifier UNIQUE (identifier),
    CONSTRAINT uk_publishers_code UNIQUE (code),
    CONSTRAINT fk_publishers_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code),
    CONSTRAINT ck_publishers_scope CHECK (publisher_scope IN ('WORKSPACE', 'APPLICATION')),
    CONSTRAINT ck_publishers_code CHECK (code REGEXP '^[A-Z][A-Z0-9_]{0,39}$')
);

INSERT INTO schema_types
    (version, identifier, code, name, description, lifecycle_code, scope_code, created_at, updated_at)
VALUES
    (0, UUID(), 'PUBLISHER_WEB_SOCKET', 'Web Socket publisher', 'Platform schema for Web Socket publishers', 'ACTIVE', 'PLATFORM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (0, UUID(), 'PUBLISHER_KAAS', 'KAAS publisher', 'Platform schema for KAAS publishers', 'ACTIVE', 'PLATFORM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (0, UUID(), 'PUBLISHER_APPCONFIG', 'AppConfig publisher', 'Platform schema for AppConfig publishers', 'ACTIVE', 'PLATFORM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
