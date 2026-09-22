INSERT IGNORE INTO type_life_cycle
    (code, label, description, sort_order, is_active, settings)
VALUES
    ('ACTIVE', 'Active', 'Active lifecycle state', 1, true, '{}'),
    ('INACTIVE', 'Inactive', 'Inactive lifecycle state', 2, true, '{}'),
    ('PENDING_DELETION', 'Pending deletion', 'Pending physical deletion', 3, true, '{}');

ALTER TABLE workspaces
    RENAME COLUMN lifecycle TO lifecycle_code;

ALTER TABLE workspaces
    MODIFY COLUMN lifecycle_code VARCHAR(50) NOT NULL,
    ADD CONSTRAINT fk_workspaces_lifecycle
        FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code);
