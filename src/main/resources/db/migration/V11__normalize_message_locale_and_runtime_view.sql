ALTER TABLE message_translations
MODIFY COLUMN locale VARCHAR(35) NOT NULL;

UPDATE message_translations
SET
    locale =
REPLACE
    (locale, '_', '-')
WHERE
    locale LIKE '%\_%' ESCAPE '\\';

CREATE OR REPLACE VIEW vw_platform_messages AS
SELECT
    CONCAT(m.service_code, '.', m.message_key) AS message_key,
    m.code AS code,
    m.http_status AS http_status,
    mt.locale AS locale,
    mt.title AS title,
    mt.detail AS message,
    mt.suggestion AS solution
FROM
    messages m
    JOIN type_services s ON s.code = m.service_code
    JOIN message_translations mt ON mt.message_id = m.id
WHERE
    s.is_active = TRUE
    AND m.lifecycle_code = 'ACTIVE'
    AND mt.lifecycle_code = 'ACTIVE';
