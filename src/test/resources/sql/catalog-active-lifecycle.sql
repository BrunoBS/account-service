-- Required lifecycle root for catalog rows; each catalog operation is created through HTTP.
INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings)
VALUES ('ACTIVE', 'Active lifecycle state', 'Active lifecycle state', 1, true, '{}');
