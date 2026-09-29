CREATE TABLE platform_services
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    version        BIGINT       NOT NULL DEFAULT 0,
    identifier     VARCHAR(36)  NOT NULL,
    code           VARCHAR(50)  NOT NULL,
    name           VARCHAR(100) NOT NULL,
    description    VARCHAR(500),
    lifecycle_code VARCHAR(50)  NOT NULL,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL,
    CONSTRAINT pk_platform_services PRIMARY KEY (id),
    CONSTRAINT uk_platform_services_identifier UNIQUE (identifier),
    CONSTRAINT uk_platform_services_code UNIQUE (code),
    CONSTRAINT fk_platform_services_lifecycle
        FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle (code)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE platform_features
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    version        BIGINT       NOT NULL DEFAULT 0,
    identifier     VARCHAR(36)  NOT NULL,
    code           VARCHAR(50)  NOT NULL,
    name           VARCHAR(100) NOT NULL,
    description    VARCHAR(500),
    service_id     BIGINT       NOT NULL,
    lifecycle_code VARCHAR(50)  NOT NULL,
    settings       JSON         NOT NULL,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP    NOT NULL,
    CONSTRAINT pk_platform_features PRIMARY KEY (id),
    CONSTRAINT uk_platform_features_identifier UNIQUE (identifier),
    CONSTRAINT uk_platform_features_code UNIQUE (code),
    CONSTRAINT fk_platform_features_service
        FOREIGN KEY (service_id) REFERENCES platform_services (id),
    CONSTRAINT fk_platform_features_lifecycle
        FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle (code)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE platform_feature_scopes
(
    feature_id         BIGINT      NOT NULL,
    feature_scope_code VARCHAR(50) NOT NULL,
    CONSTRAINT pk_platform_feature_scopes PRIMARY KEY (feature_id, feature_scope_code),
    CONSTRAINT fk_platform_feature_scopes_feature
        FOREIGN KEY (feature_id) REFERENCES platform_features (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_platform_feature_scopes_scope
        FOREIGN KEY (feature_scope_code) REFERENCES type_feature_scopes (code)
) DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ServiceType becomes the Platform Service entity while preserving every legacy service code.
INSERT INTO platform_services
(identifier, code, name, description, lifecycle_code, created_at, updated_at)
SELECT UUID(),
       s.code,
       s.label,
       s.description,
       CASE WHEN s.is_active THEN 'ACTIVE' ELSE 'INACTIVE' END,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM type_services s;

-- FeatureType becomes the Platform Feature entity.
-- Preserve a valid legacy owner when one was stored in settings; workspace-service is the
-- compatibility fallback for records created before ownership became structural.
-- Operational settings are preserved, while former structural keys are removed.
INSERT INTO platform_features
(identifier, code, name, description, service_id, lifecycle_code, settings, created_at, updated_at)
SELECT UUID(),
       f.code,
       f.label,
       f.description,
       COALESCE(owner_service.id, fallback_service.id),
       CASE WHEN f.is_active THEN 'ACTIVE' ELSE 'INACTIVE' END,
       CASE
           WHEN JSON_VALID(f.settings)
               AND JSON_EXTRACT(f.settings, '$.quarantine') IS NOT NULL
               AND JSON_EXTRACT(f.settings, '$.audit') IS NOT NULL
               AND JSON_EXTRACT(f.settings, '$.purge') IS NOT NULL
               THEN JSON_REMOVE(CAST(f.settings AS JSON), '$.service', '$.scopes')
           ELSE JSON_OBJECT(
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
                )
           END,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM type_features f
         LEFT JOIN platform_services fallback_service
                   ON fallback_service.code COLLATE utf8mb4_unicode_ci = 'workspace-service' COLLATE utf8mb4_unicode_ci
         LEFT JOIN platform_services owner_service
                   ON JSON_VALID(f.settings)
                       AND owner_service.code COLLATE utf8mb4_unicode_ci = JSON_UNQUOTE(JSON_EXTRACT(f.settings, '$.service')) COLLATE utf8mb4_unicode_ci;

-- Promote both historical single-scope and array-scope representations to the explicit N:N relation.
INSERT
IGNORE INTO platform_feature_scopes (feature_id, feature_scope_code)
SELECT pf.id, fs.code
FROM type_features legacy
         JOIN platform_features pf ON pf.code COLLATE utf8mb4_unicode_ci = legacy.code COLLATE utf8mb4_unicode_ci
         JOIN type_feature_scopes fs
              ON fs.code COLLATE utf8mb4_unicode_ci = JSON_UNQUOTE(JSON_EXTRACT(legacy.settings, '$.scopes')) COLLATE utf8mb4_unicode_ci
WHERE JSON_VALID(legacy.settings)
  AND JSON_TYPE(JSON_EXTRACT(legacy.settings, '$.scopes')) = 'STRING';

INSERT
IGNORE INTO platform_feature_scopes (feature_id, feature_scope_code)
SELECT pf.id, fs.code
FROM type_features legacy
         JOIN platform_features pf ON pf.code COLLATE utf8mb4_unicode_ci = legacy.code COLLATE utf8mb4_unicode_ci
         JOIN JSON_TABLE(
        CASE
            WHEN JSON_VALID(legacy.settings)
                AND JSON_TYPE(JSON_EXTRACT(legacy.settings, '$.scopes')) = 'ARRAY'
                THEN JSON_EXTRACT(legacy.settings, '$.scopes')
            ELSE JSON_ARRAY()
            END,
        '$[*]' COLUMNS(scope_code VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci PATH '$')
              ) legacy_scope
         JOIN type_feature_scopes fs ON fs.code COLLATE utf8mb4_unicode_ci = legacy_scope.scope_code COLLATE utf8mb4_unicode_ci;

-- Message Management now validates and references the Platform Service entity.
ALTER TABLE messages
DROP
FOREIGN KEY fk_messages_service;

ALTER TABLE messages
    ADD CONSTRAINT fk_messages_platform_service
        FOREIGN KEY (service_code) REFERENCES platform_services (code);

CREATE
OR REPLACE VIEW vw_platform_messages AS
SELECT CONCAT(m.service_code, '.', m.message_key) AS message_key,
       m.code                                     AS code,
       m.http_status                              AS http_status,
       mt.locale                                  AS locale,
       mt.title                                   AS title,
       mt.detail                                  AS message,
       mt.suggestion                              AS solution
FROM messages m
         JOIN platform_services s
              ON s.code COLLATE utf8mb4_unicode_ci = m.service_code COLLATE utf8mb4_unicode_ci
         JOIN message_translations mt
              ON mt.message_id = m.id
WHERE s.lifecycle_code = 'ACTIVE'
  AND m.lifecycle_code = 'ACTIVE'
  AND mt.lifecycle_code = 'ACTIVE';

CREATE
OR REPLACE VIEW vw_feature_runtime_config AS
SELECT f.code                                                                                 AS feature_code,
       f.name                                                                                 AS feature_label,
       s.code                                                                                 AS service_code,
       s.name                                                                                 AS service_label,
       JSON_EXTRACT(f.settings, '$.quarantine.enabled') = true                                AS quarantine_enabled,
       CAST(JSON_UNQUOTE(JSON_EXTRACT(f.settings, '$.quarantine.retentionDays')) AS UNSIGNED) AS quarantine_retention_days,
       JSON_EXTRACT(f.settings, '$.quarantine.restoreAllowed') = true                         AS quarantine_restore_allowed,
       JSON_EXTRACT(f.settings, '$.audit.enabled') = true                                     AS audit_enabled,
       JSON_EXTRACT(f.settings, '$.audit.snapshotOnPurge') = true                             AS audit_snapshot_on_purge,
       JSON_EXTRACT(f.settings, '$.purge.enabled') = true                                     AS purge_enabled,
       f.lifecycle_code = 'ACTIVE'                                                            AS feature_active,
       s.lifecycle_code = 'ACTIVE'                                                            AS service_active
FROM platform_features f
         JOIN platform_services s
              ON s.id = f.service_id;

DROP TABLE type_features;
DROP TABLE type_services;
