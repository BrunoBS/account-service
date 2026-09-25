CREATE TABLE platform_services (
    id BIGINT NOT NULL AUTO_INCREMENT,
    identifier VARCHAR(36) NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    lifecycle_code VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_platform_services PRIMARY KEY (id),
    CONSTRAINT uk_platform_services_identifier UNIQUE (identifier),
    CONSTRAINT uk_platform_services_code UNIQUE (code),
    CONSTRAINT fk_platform_services_lifecycle
        FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code)
);

CREATE TABLE platform_features (
    id BIGINT NOT NULL AUTO_INCREMENT,
    identifier VARCHAR(36) NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    service_id BIGINT NOT NULL,
    lifecycle_code VARCHAR(50) NOT NULL,
    settings JSON NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_platform_features PRIMARY KEY (id),
    CONSTRAINT uk_platform_features_identifier UNIQUE (identifier),
    CONSTRAINT uk_platform_features_code UNIQUE (code),
    CONSTRAINT fk_platform_features_service
        FOREIGN KEY (service_id) REFERENCES platform_services(id),
    CONSTRAINT fk_platform_features_lifecycle
        FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code)
);

CREATE TABLE platform_feature_scopes (
    feature_id BIGINT NOT NULL,
    feature_scope_code VARCHAR(50) NOT NULL,
    CONSTRAINT pk_platform_feature_scopes PRIMARY KEY (feature_id, feature_scope_code),
    CONSTRAINT fk_platform_feature_scopes_feature
        FOREIGN KEY (feature_id) REFERENCES platform_features(id),
    CONSTRAINT fk_platform_feature_scopes_scope
        FOREIGN KEY (feature_scope_code) REFERENCES type_feature_scopes(code)
);

INSERT INTO platform_services
    (identifier, code, name, description, lifecycle_code, created_at, updated_at)
VALUES
    (UUID(), 'workspace-service', 'Workspace Service',
     'Owner service for workspace platform features', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO platform_features
    (identifier, code, name, description, service_id, lifecycle_code, settings, created_at, updated_at)
SELECT
    UUID(),
    f.code,
    f.label,
    f.description,
    s.id,
    CASE WHEN f.is_active THEN 'ACTIVE' ELSE 'INACTIVE' END,
    JSON_OBJECT(
        'quarantine', JSON_OBJECT(
            'enabled', true,
            'retentionDays', CASE WHEN f.code = 'MESSAGE' THEN 0 ELSE 30 END,
            'restoreAllowed', CASE WHEN f.code = 'MESSAGE' THEN false ELSE true END
        ),
        'audit', JSON_OBJECT(
            'enabled', true,
            'snapshotOnPurge', CASE WHEN f.code = 'MESSAGE' THEN false ELSE true END
        ),
        'purge', JSON_OBJECT('enabled', true)
    ),
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM type_features f
JOIN platform_services s ON s.code = 'workspace-service';

CREATE OR REPLACE VIEW vw_feature_runtime_config AS
SELECT
    f.code AS feature_code,
    f.name AS feature_label,
    s.code AS service_code,
    s.name AS service_label,
    JSON_EXTRACT(f.settings, '$.quarantine.enabled') = true AS quarantine_enabled,
    CAST(JSON_UNQUOTE(JSON_EXTRACT(f.settings, '$.quarantine.retentionDays')) AS UNSIGNED) AS quarantine_retention_days,
    JSON_EXTRACT(f.settings, '$.quarantine.restoreAllowed') = true AS quarantine_restore_allowed,
    JSON_EXTRACT(f.settings, '$.audit.enabled') = true AS audit_enabled,
    JSON_EXTRACT(f.settings, '$.audit.snapshotOnPurge') = true AS audit_snapshot_on_purge,
    JSON_EXTRACT(f.settings, '$.purge.enabled') = true AS purge_enabled,
    f.lifecycle_code = 'ACTIVE' AS feature_active,
    s.lifecycle_code = 'ACTIVE' AS service_active
FROM platform_features f
JOIN platform_services s ON s.id = f.service_id;
