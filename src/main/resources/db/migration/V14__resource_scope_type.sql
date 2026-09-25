CREATE TABLE type_resource_scopes (
    code VARCHAR(50) NOT NULL,
    label VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order INT NOT NULL,
    is_active BOOLEAN NOT NULL,
    settings TEXT NOT NULL,
    CONSTRAINT pk_type_resource_scopes PRIMARY KEY (code)
);

INSERT IGNORE INTO type_resource_scopes
    (code, label, description, sort_order, is_active, settings)
SELECT
    code,
    label,
    description,
    sort_order,
    is_active,
    settings
FROM type_publisher_scopes;

INSERT IGNORE INTO type_resource_scopes
    (code, label, description, sort_order, is_active, settings)
SELECT
    code,
    label,
    description,
    sort_order,
    is_active,
    settings
FROM type_schemas;

DROP TABLE type_publisher_scopes;
DROP TABLE type_schemas;
