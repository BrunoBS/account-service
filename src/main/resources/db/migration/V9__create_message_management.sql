CREATE TABLE type_services (
    code VARCHAR(50) NOT NULL,
    label VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order INT NOT NULL,
    is_active BOOLEAN NOT NULL,
    settings TEXT NOT NULL,
    CONSTRAINT pk_type_services PRIMARY KEY (code)
);

INSERT INTO
    type_services (
        code,
        label,
        description,
        sort_order,
        is_active,
        settings
    )
VALUES
    (
        'workspace-service',
        'Workspace Service',
        'Workspace and platform administration service',
        1,
        true,
        '{}'
    );

CREATE TABLE messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL,
    service_code VARCHAR(50) NOT NULL,
    message_key VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL,
    http_status INT NOT NULL,
    lifecycle_code VARCHAR(50) NOT NULL,
    observation VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_messages PRIMARY KEY (id),
    CONSTRAINT uk_messages_identifier UNIQUE (identifier),
    CONSTRAINT uk_messages_service_message_key UNIQUE (service_code, message_key),
    CONSTRAINT uk_messages_service_code UNIQUE (service_code, code),
    CONSTRAINT fk_messages_service FOREIGN KEY (service_code) REFERENCES type_services (code),
    CONSTRAINT fk_messages_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle (code),
    CONSTRAINT ck_messages_http_status CHECK (http_status BETWEEN 100 AND 599)
);

CREATE TABLE message_translations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL,
    message_id BIGINT NOT NULL,
    locale VARCHAR(10) NOT NULL,
    title VARCHAR(150) NOT NULL,
    detail VARCHAR(1000) NOT NULL,
    suggestion VARCHAR(1000) NOT NULL,
    lifecycle_code VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_message_translations PRIMARY KEY (id),
    CONSTRAINT uk_message_translations_identifier UNIQUE (identifier),
    CONSTRAINT uk_message_translations_message_locale UNIQUE (message_id, locale),
    CONSTRAINT fk_message_translations_message FOREIGN KEY (message_id) REFERENCES messages (id) ON DELETE CASCADE,
    CONSTRAINT fk_message_translations_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle (code)
);

CREATE INDEX idx_messages_service ON messages (service_code);

CREATE INDEX idx_messages_lifecycle ON messages (lifecycle_code);

CREATE INDEX idx_message_translations_lifecycle ON message_translations (lifecycle_code);

CREATE OR REPLACE VIEW vw_platform_messages AS
SELECT
    m.service_code AS service,
    m.message_key AS message_key,
    m.code AS code,
    m.http_status AS http_status,
    mt.locale AS locale,
    mt.title AS title,
    mt.detail AS detail,
    mt.suggestion AS suggestion
FROM
    messages m
    JOIN type_services s ON s.code = m.service_code
    JOIN message_translations mt ON mt.message_id = m.id
WHERE
    s.is_active = TRUE
    AND m.lifecycle_code = 'ACTIVE'
    AND mt.lifecycle_code = 'ACTIVE';
