DROP VIEW IF EXISTS vw_platform_messages;

ALTER TABLE messages
    ADD COLUMN service_identifier VARCHAR(36) NULL AFTER identifier;

UPDATE messages m
JOIN platform_services s
  ON s.id = m.service_id
SET m.service_identifier = s.identifier;

ALTER TABLE messages
    MODIFY COLUMN service_identifier VARCHAR(36) NOT NULL,
    DROP FOREIGN KEY fk_messages_platform_service,
    DROP INDEX uk_messages_service_message_key,
    DROP INDEX uk_messages_service_message_code,
    DROP INDEX idx_messages_service,
    DROP COLUMN service_id;

ALTER TABLE messages
    ADD CONSTRAINT uk_messages_service_message_key
        UNIQUE (service_identifier, message_key),
    ADD CONSTRAINT uk_messages_service_message_code
        UNIQUE (service_identifier, code);

CREATE INDEX idx_messages_service_identifier
    ON messages (service_identifier);

CREATE OR REPLACE VIEW vw_platform_messages AS
SELECT
    CONCAT(s.code, '.', m.message_key) AS message_key,
    m.code AS code,
    m.http_status AS http_status,
    mt.locale AS locale,
    mt.title AS title,
    mt.detail AS message,
    mt.suggestion AS solution
FROM messages m
JOIN platform_services s
  ON s.identifier = m.service_identifier
JOIN message_translations mt
  ON mt.message_id = m.id
WHERE s.lifecycle_code = 'ACTIVE'
  AND m.lifecycle_code = 'ACTIVE'
  AND mt.lifecycle_code = 'ACTIVE';
