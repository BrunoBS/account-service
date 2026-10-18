CREATE TABLE type_configuration_types (
    code VARCHAR(50) NOT NULL,
    label VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order INT NOT NULL,
    is_active BOOLEAN NOT NULL,
    settings TEXT NOT NULL,
    CONSTRAINT pk_type_configuration_types PRIMARY KEY (code),
    CONSTRAINT ck_type_configuration_types_code CHECK (code IN ('CONFIGURATION', 'EXPRESSION'))
);

INSERT INTO type_configuration_types (code, label, description, sort_order, is_active, settings) VALUES
('CONFIGURATION', 'Configuration', NULL, 1, true, '{}'),
('EXPRESSION', 'Expression', NULL, 2, true, '{}');

CREATE TABLE type_configuration_statuses (
    code VARCHAR(50) NOT NULL,
    label VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order INT NOT NULL,
    is_active BOOLEAN NOT NULL,
    settings TEXT NOT NULL,
    CONSTRAINT pk_type_configuration_statuses PRIMARY KEY (code),
    CONSTRAINT ck_type_configuration_statuses_code CHECK (code IN ('DRAFT', 'PUBLISHED'))
);

INSERT INTO type_configuration_statuses (code, label, description, sort_order, is_active, settings) VALUES
('DRAFT', 'Draft', NULL, 1, true, '{}'),
('PUBLISHED', 'Published', NULL, 2, true, '{}');

-- Keep an existing global registration intact, including its UUID and settings.
INSERT INTO platform_microservices
    (version, identifier, code, name, description, settings, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'platform', 'Platform', 'Global platform configuration namespace', '{}',
       'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM platform_microservices WHERE code = 'platform');
