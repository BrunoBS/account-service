CREATE TABLE type_services (
    code VARCHAR(50) NOT NULL,
    label VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order INT NOT NULL,
    is_active BOOLEAN NOT NULL,
    settings JSON NOT NULL,
    CONSTRAINT pk_type_services PRIMARY KEY (code)
);

INSERT INTO type_services
    (code, label, description, sort_order, is_active, settings)
VALUES
    ('workspace-service', 'Workspace Service', 'Owner service for workspace platform features', 1, true, JSON_OBJECT());

-- FeatureType becomes a simple catalog. Ownership and operational policy live in settings.
ALTER TABLE type_features
    DROP FOREIGN KEY fk_type_features_scope,
    DROP INDEX uk_type_features_scope_name,
    DROP COLUMN type_feature_scopes_id,
    DROP COLUMN available;

ALTER TABLE type_features
    MODIFY COLUMN settings JSON NOT NULL;

UPDATE type_features
SET settings = JSON_OBJECT(
    'service', 'workspace-service',
    'quarantine', JSON_OBJECT(
        'enabled', true,
        'retentionDays', CASE WHEN code = 'MESSAGE' THEN 0 ELSE 30 END,
        'restoreAllowed', CASE WHEN code = 'MESSAGE' THEN false ELSE true END
    ),
    'audit', JSON_OBJECT(
        'enabled', true,
        'snapshotOnPurge', CASE WHEN code = 'MESSAGE' THEN false ELSE true END
    ),
    'purge', JSON_OBJECT('enabled', true)
);

CREATE OR REPLACE VIEW vw_feature_runtime_config AS
SELECT
    f.code AS feature_code,
    f.label AS feature_label,
    JSON_UNQUOTE(JSON_EXTRACT(f.settings, '$.service')) AS service_code,
    s.label AS service_label,
    CAST(JSON_UNQUOTE(JSON_EXTRACT(f.settings, '$.quarantine.enabled')) AS UNSIGNED) AS quarantine_enabled,
    CAST(JSON_UNQUOTE(JSON_EXTRACT(f.settings, '$.quarantine.retentionDays')) AS UNSIGNED) AS quarantine_retention_days,
    CAST(JSON_UNQUOTE(JSON_EXTRACT(f.settings, '$.quarantine.restoreAllowed')) AS UNSIGNED) AS quarantine_restore_allowed,
    CAST(JSON_UNQUOTE(JSON_EXTRACT(f.settings, '$.audit.enabled')) AS UNSIGNED) AS audit_enabled,
    CAST(JSON_UNQUOTE(JSON_EXTRACT(f.settings, '$.audit.snapshotOnPurge')) AS UNSIGNED) AS audit_snapshot_on_purge,
    CAST(JSON_UNQUOTE(JSON_EXTRACT(f.settings, '$.purge.enabled')) AS UNSIGNED) AS purge_enabled,
    f.is_active AS feature_active,
    s.is_active AS service_active
FROM type_features f
JOIN type_services s
  ON s.code = JSON_UNQUOTE(JSON_EXTRACT(f.settings, '$.service'));
