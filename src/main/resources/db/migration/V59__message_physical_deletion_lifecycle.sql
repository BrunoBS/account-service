-- Preserve legacy quarantined records as inactive; future DELETE removes them physically.
UPDATE messages
SET lifecycle_code = 'INACTIVE', version = version + 1, updated_at = CURRENT_TIMESTAMP(6)
WHERE lifecycle_code = 'QUARANTINED';

UPDATE message_translations
SET lifecycle_code = 'INACTIVE', version = version + 1, updated_at = CURRENT_TIMESTAMP(6)
WHERE lifecycle_code = 'QUARANTINED';

ALTER TABLE messages
    ADD CONSTRAINT ck_messages_supported_lifecycle
        CHECK (lifecycle_code IN ('ACTIVE', 'INACTIVE'));

ALTER TABLE message_translations
    ADD CONSTRAINT ck_message_translations_supported_lifecycle
        CHECK (lifecycle_code IN ('ACTIVE', 'INACTIVE'));
