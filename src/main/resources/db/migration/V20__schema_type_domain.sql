ALTER TABLE schema_definitions
DROP FOREIGN KEY fk_schema_definitions_type;

RENAME TABLE type_schema_types TO schema_types;

ALTER TABLE schema_types
ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT FIRST,
ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER id,
ADD COLUMN identifier VARCHAR(36) NULL AFTER version,
ADD COLUMN lifecycle_code VARCHAR(50) NULL AFTER settings,
ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER lifecycle_code,
ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER created_at,
ADD UNIQUE KEY uk_schema_types_id (id);

UPDATE schema_types
SET
    identifier = UUID(),
    lifecycle_code = CASE
        WHEN is_active THEN 'ACTIVE'
        ELSE 'INACTIVE'
    END;

ALTER TABLE schema_types
MODIFY COLUMN identifier VARCHAR(36) NOT NULL,
MODIFY COLUMN lifecycle_code VARCHAR(50) NOT NULL,
CHANGE COLUMN label name VARCHAR(100) NOT NULL,
DROP PRIMARY KEY,
DROP COLUMN sort_order,
DROP COLUMN is_active,
DROP COLUMN settings,
ADD CONSTRAINT pk_schema_types PRIMARY KEY (id),
ADD CONSTRAINT uk_schema_types_identifier UNIQUE (identifier),
ADD CONSTRAINT uk_schema_types_code UNIQUE (code),
ADD CONSTRAINT fk_schema_types_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle (code);

CREATE TABLE schema_type_scopes (
    schema_type_id BIGINT NOT NULL,
    scope_code VARCHAR(50) NOT NULL,
    CONSTRAINT pk_schema_type_scopes PRIMARY KEY (schema_type_id, scope_code),
    CONSTRAINT fk_schema_type_scopes_type FOREIGN KEY (schema_type_id) REFERENCES schema_types (id),
    CONSTRAINT fk_schema_type_scopes_scope FOREIGN KEY (scope_code) REFERENCES type_schema_scopes (code)
);

INSERT INTO
    schema_type_scopes (schema_type_id, scope_code)
SELECT
    st.id,
    ss.code
FROM
    schema_types st
    CROSS JOIN type_schema_scopes ss
WHERE
    ss.code IN ('PLATFORM', 'WORKSPACE');

ALTER TABLE schema_definitions
ADD CONSTRAINT fk_schema_definitions_type FOREIGN KEY (schema_type_code) REFERENCES schema_types (code);

ALTER TABLE schema_types
DROP INDEX uk_schema_types_id;
