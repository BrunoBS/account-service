INSERT IGNORE INTO
    type_life_cycle (
        code,
        label,
        description,
        sort_order,
        is_active,
        settings
    )
VALUES
    (
        'QUARANTINED',
        'Quarantined',
        'Quarantined lifecycle state',
        3,
        true,
        '{}'
    );

UPDATE workspaces
SET
    lifecycle_code = 'QUARANTINED'
WHERE
    lifecycle_code = 'PENDING_DELETION';

UPDATE messages
SET
    lifecycle_code = 'QUARANTINED'
WHERE
    lifecycle_code = 'PENDING_DELETION';

UPDATE message_translations
SET
    lifecycle_code = 'QUARANTINED'
WHERE
    lifecycle_code = 'PENDING_DELETION';

DELETE FROM type_life_cycle
WHERE
    code = 'PENDING_DELETION';
