-- Upgrade existing installations without recreating microservices, features or messages.
DROP VIEW IF EXISTS vw_platform_messages;
DROP VIEW IF EXISTS vw_feature_runtime_config;

ALTER TABLE platform_features DROP FOREIGN KEY fk_platform_features_service;
ALTER TABLE messages DROP FOREIGN KEY fk_messages_platform_service;

RENAME TABLE platform_services TO platform_microservices;

ALTER TABLE platform_microservices
    RENAME INDEX uk_platform_services_identifier TO uk_platform_microservices_identifier,
    RENAME INDEX uk_platform_services_code TO uk_platform_microservices_code,
    RENAME INDEX uk_platform_services_name TO uk_platform_microservices_name,
    DROP CHECK ck_platform_services_code,
    ADD CONSTRAINT ck_platform_microservices_code
        CHECK (code REGEXP '^[a-z][a-z0-9]*(-[a-z0-9]+)*$');

ALTER TABLE platform_features
    CHANGE COLUMN service_id microservice_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_platform_features_microservice
        FOREIGN KEY (microservice_id) REFERENCES platform_microservices(id);

ALTER TABLE messages
    DROP INDEX uk_messages_service_message_key,
    DROP INDEX uk_messages_service_message_code,
    DROP INDEX idx_messages_service,
    CHANGE COLUMN service_id microservice_id BIGINT NOT NULL,
    ADD CONSTRAINT uk_messages_microservice_message_key UNIQUE (microservice_id, message_key),
    ADD CONSTRAINT uk_messages_microservice_message_code UNIQUE (microservice_id, code),
    ADD CONSTRAINT fk_messages_platform_microservice
        FOREIGN KEY (microservice_id) REFERENCES platform_microservices(id);

CREATE INDEX idx_messages_microservice ON messages (microservice_id);

CREATE VIEW vw_platform_messages AS
SELECT
    CONCAT(s.code, '.', m.message_key) AS message_key,
    m.code AS code,
    m.http_status AS http_status,
    mt.locale AS locale,
    mt.title AS title,
    mt.detail AS message,
    mt.suggestion AS solution
FROM messages m
JOIN platform_microservices s ON s.id = m.microservice_id
JOIN message_translations mt ON mt.message_id = m.id
WHERE s.lifecycle_code = 'ACTIVE'
  AND m.lifecycle_code = 'ACTIVE'
  AND mt.lifecycle_code = 'ACTIVE';

CREATE VIEW vw_feature_runtime_config AS
SELECT
    f.code AS feature_code,
    f.name AS feature_label,
    s.code AS microservice_code,
    s.name AS microservice_label,
    JSON_EXTRACT(f.settings, '$.quarantine.enabled') = true AS quarantine_enabled,
    CAST(JSON_UNQUOTE(JSON_EXTRACT(f.settings, '$.quarantine.retentionDays')) AS UNSIGNED) AS quarantine_retention_days,
    JSON_EXTRACT(f.settings, '$.quarantine.restoreAllowed') = true AS quarantine_restore_allowed,
    JSON_EXTRACT(f.settings, '$.audit.enabled') = true AS audit_enabled,
    JSON_EXTRACT(f.settings, '$.audit.snapshotOnPurge') = true AS audit_snapshot_on_purge,
    JSON_EXTRACT(f.settings, '$.purge.enabled') = true AS purge_enabled,
    f.lifecycle_code = 'ACTIVE' AS feature_active,
    s.lifecycle_code = 'ACTIVE' AS microservice_active
FROM platform_features f
JOIN platform_microservices s ON s.id = f.microservice_id;

-- Application settings will own these concepts when Application is introduced.
DROP TABLE type_languages;
DROP TABLE type_infrastructures;
