INSERT INTO type_workspaces (code, label, description, sort_order, is_active, settings)
VALUES
    ('ADMIN', 'Admin', 'Administrative workspace', 1, true, '{}'),
    ('MANAGER', 'Manager', 'Management workspace', 2, true, '{}'),
    ('CATALOG', 'Catalog', 'Catalog workspace', 3, true, '{}')
ON DUPLICATE KEY UPDATE
    label = VALUES(label),
    description = VALUES(description),
    sort_order = VALUES(sort_order),
    is_active = VALUES(is_active),
    settings = VALUES(settings);
