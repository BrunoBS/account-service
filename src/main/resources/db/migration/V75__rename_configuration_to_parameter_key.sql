RENAME TABLE configurations TO parameter_keys,
             type_configuration_types TO type_parameter_key_types,
             type_configuration_statuses TO type_parameter_key_statuses;

ALTER TABLE parameter_keys
    RENAME COLUMN configuration_key TO parameter_key,
    RENAME INDEX uk_configuration_identifier TO uk_parameter_key_identifier,
    RENAME INDEX uk_configuration_microservice_key TO uk_parameter_key_microservice_key,
    RENAME INDEX idx_configuration_active_id TO idx_parameter_key_active_id,
    DROP FOREIGN KEY fk_configuration_microservice,
    DROP FOREIGN KEY fk_configuration_type,
    DROP FOREIGN KEY fk_configuration_published_type,
    DROP FOREIGN KEY fk_configuration_status,
    DROP CHECK ck_configuration_key,
    DROP CHECK ck_configuration_snapshot,
    DROP CHECK ck_configuration_inactive,
    DROP CHECK ck_configuration_published,
    ADD CONSTRAINT fk_parameter_key_microservice FOREIGN KEY (microservice_id) REFERENCES platform_microservices(id),
    ADD CONSTRAINT fk_parameter_key_type FOREIGN KEY (type_code) REFERENCES type_parameter_key_types(code),
    ADD CONSTRAINT fk_parameter_key_published_type FOREIGN KEY (published_type_code) REFERENCES type_parameter_key_types(code),
    ADD CONSTRAINT fk_parameter_key_status FOREIGN KEY (status_code) REFERENCES type_parameter_key_statuses(code),
    ADD CONSTRAINT ck_parameter_key_key CHECK (REGEXP_LIKE(parameter_key, '^[a-z0-9]+([._-][a-z0-9]+)*$', 'c')),
    ADD CONSTRAINT ck_parameter_key_snapshot CHECK (
        (published_value IS NULL AND published_type_code IS NULL)
        OR (published_value IS NOT NULL AND published_type_code IS NOT NULL)),
    ADD CONSTRAINT ck_parameter_key_inactive CHECK (
        active = true OR (status_code = 'DRAFT' AND published_value IS NULL AND published_type_code IS NULL)),
    ADD CONSTRAINT ck_parameter_key_published CHECK (
        status_code <> 'PUBLISHED' OR (active = true AND published_value IS NOT NULL));

ALTER TABLE type_parameter_key_types
    DROP CHECK ck_type_configuration_types_code,
    ADD CONSTRAINT ck_type_parameter_key_types_code CHECK (code IN ('CONFIGURATION', 'EXPRESSION'));
ALTER TABLE type_parameter_key_statuses
    DROP CHECK ck_type_configuration_statuses_code,
    ADD CONSTRAINT ck_type_parameter_key_statuses_code CHECK (code IN ('DRAFT', 'PUBLISHED'));
