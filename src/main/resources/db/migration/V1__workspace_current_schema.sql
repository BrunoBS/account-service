-- Golden Platform workspace-service current schema baseline.
-- Compiled from the current ordered schema definition on the test branch.

-- Current schema statements sourced from V1__create_account_schema.sql
CREATE TABLE accounts
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    version          BIGINT       NOT NULL DEFAULT 0,
    identifier       VARCHAR(36)  NOT NULL,
    account_type     VARCHAR(20)  NOT NULL,
    name             VARCHAR(100) NOT NULL,
    description      VARCHAR(500) NOT NULL,
    requester        VARCHAR(255) NOT NULL,
    acronym          VARCHAR(5)   NOT NULL,
    settings         TEXT NULL,
    authorizer_group VARCHAR(255) NULL,
    email_group      VARCHAR(320) NOT NULL,
    onboarding       BOOLEAN      NOT NULL DEFAULT FALSE,
    lifecycle        VARCHAR(20)  NOT NULL,
    created_at       DATETIME(6) NOT NULL,
    updated_at       DATETIME(6) NOT NULL,
    CONSTRAINT pk_accounts PRIMARY KEY (id),
    CONSTRAINT uk_accounts_identifier UNIQUE (identifier),
    CONSTRAINT uk_accounts_name UNIQUE (name),
    CONSTRAINT ck_accounts_account_type CHECK (account_type IN ('ADMIN', 'MANAGER')),
    CONSTRAINT ck_accounts_lifecycle CHECK (lifecycle IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE account_approvers
(
    id         VARCHAR(36)  NOT NULL,
    account_id BIGINT       NOT NULL,
    functional VARCHAR(255) NOT NULL,
    email      VARCHAR(320) NOT NULL,
    CONSTRAINT pk_account_approvers PRIMARY KEY (id),
    CONSTRAINT fk_account_approvers_account
        FOREIGN KEY (account_id) REFERENCES accounts (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_account_approvers_account_id
    ON account_approvers (account_id);

CREATE INDEX idx_accounts_lifecycle
    ON accounts (lifecycle);

CREATE INDEX idx_accounts_account_type
    ON accounts (account_type);


-- Current schema statements sourced from V2__create_tags.sql
CREATE TABLE tags
(
    id          VARCHAR(36)  NOT NULL,
    owner_type  VARCHAR(50)  NOT NULL,
    owner_id    VARCHAR(100) NOT NULL,
    name        VARCHAR(150) NOT NULL,
    origin_type VARCHAR(20)  NOT NULL,
    CONSTRAINT pk_tags PRIMARY KEY (id),
    CONSTRAINT uk_tags_owner_name UNIQUE (owner_type, owner_id, name),
    CONSTRAINT ck_tags_origin_type CHECK (origin_type IN ('MANUAL', 'SYSTEM'))
);

CREATE INDEX idx_tags_owner
    ON tags (owner_type, owner_id);

CREATE INDEX idx_tags_lookup
    ON tags (owner_type, name);


-- Current schema statements sourced from V3__rename_account_domain_to_workspace.sql
ALTER TABLE account_approvers
DROP
FOREIGN KEY fk_account_approvers_account;

RENAME
TABLE
    accounts TO workspaces,
    account_approvers TO workspace_approvers;

ALTER TABLE workspaces
DROP
CHECK ck_accounts_account_type,
    DROP
CHECK ck_accounts_lifecycle,
    RENAME COLUMN account_type TO workspace_type,
    RENAME INDEX uk_accounts_identifier TO uk_workspaces_identifier,
    RENAME INDEX uk_accounts_name TO uk_workspaces_name,
    RENAME INDEX idx_accounts_lifecycle TO idx_workspaces_lifecycle,
    RENAME INDEX idx_accounts_account_type TO idx_workspaces_workspace_type;

ALTER TABLE workspaces
    ADD CONSTRAINT ck_workspaces_workspace_type
        CHECK (workspace_type IN ('ADMIN', 'MANAGER')),
    ADD CONSTRAINT ck_workspaces_lifecycle
        CHECK (lifecycle IN ('ACTIVE', 'INACTIVE'));

ALTER TABLE workspace_approvers
    RENAME COLUMN account_id TO workspace_id,
    RENAME INDEX idx_account_approvers_account_id TO idx_workspace_approvers_workspace_id;

ALTER TABLE workspace_approvers
    ADD CONSTRAINT fk_workspace_approvers_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces (id)
            ON DELETE CASCADE;

UPDATE tags
SET owner_type = 'WORKSPACE'
WHERE owner_type = 'ACCOUNT';


-- Current schema statements sourced from V4__create_catalogs.sql
ALTER TABLE workspaces
DROP
CHECK ck_workspaces_workspace_type,
    DROP
CHECK ck_workspaces_lifecycle;

ALTER TABLE workspaces
    ADD CONSTRAINT ck_workspaces_workspace_type
        CHECK (workspace_type IN ('ADMIN', 'MANAGER', 'CATALOG')),
    ADD CONSTRAINT ck_workspaces_lifecycle
        CHECK (lifecycle IN ('ACTIVE', 'INACTIVE', 'PENDING_DELETION'));

CREATE TABLE type_workspaces
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    label       VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order  INT          NOT NULL,
    is_active   BOOLEAN      NOT NULL,
    settings    TEXT         NOT NULL,
    CONSTRAINT pk_type_workspaces PRIMARY KEY (id)
);

CREATE TABLE type_application_scopes
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    label       VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order  INT          NOT NULL,
    is_active   BOOLEAN      NOT NULL,
    settings    TEXT         NOT NULL,
    CONSTRAINT pk_type_application_scopes PRIMARY KEY (id)
);

CREATE TABLE type_authorizations
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    label       VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order  INT          NOT NULL,
    is_active   BOOLEAN      NOT NULL,
    settings    TEXT         NOT NULL,
    CONSTRAINT pk_type_authorizations PRIMARY KEY (id)
);

CREATE TABLE type_feature_scopes
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    label       VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order  INT          NOT NULL,
    is_active   BOOLEAN      NOT NULL,
    settings    TEXT         NOT NULL,
    CONSTRAINT pk_type_feature_scopes PRIMARY KEY (id)
);

CREATE TABLE type_features
(
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    name                   VARCHAR(50)  NOT NULL,
    label                  VARCHAR(100) NOT NULL,
    description            TEXT,
    sort_order             INT          NOT NULL,
    is_active              BOOLEAN      NOT NULL,
    settings               TEXT         NOT NULL,
    type_feature_scopes_id BIGINT       NOT NULL,
    available              BOOLEAN      NOT NULL,
    CONSTRAINT pk_type_features PRIMARY KEY (id),
    CONSTRAINT uk_type_features_scope_name UNIQUE (type_feature_scopes_id, name),
    CONSTRAINT fk_type_features_scope FOREIGN KEY (type_feature_scopes_id) REFERENCES type_feature_scopes (id)
);

CREATE TABLE type_infrastructures
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    label       VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order  INT          NOT NULL,
    is_active   BOOLEAN      NOT NULL,
    settings    TEXT         NOT NULL,
    CONSTRAINT pk_type_infrastructures PRIMARY KEY (id)
);

CREATE TABLE type_languages
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    label       VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order  INT          NOT NULL,
    is_active   BOOLEAN      NOT NULL,
    settings    TEXT         NOT NULL,
    CONSTRAINT pk_type_languages PRIMARY KEY (id)
);

CREATE TABLE type_life_cycle
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    label       VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order  INT          NOT NULL,
    is_active   BOOLEAN      NOT NULL,
    settings    TEXT         NOT NULL,
    CONSTRAINT pk_type_life_cycle PRIMARY KEY (id)
);

CREATE TABLE type_tag_origins
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    label       VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order  INT          NOT NULL,
    is_active   BOOLEAN      NOT NULL,
    settings    TEXT         NOT NULL,
    CONSTRAINT pk_type_tag_origins PRIMARY KEY (id)
);

CREATE TABLE type_visibilities
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    label       VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order  INT          NOT NULL,
    is_active   BOOLEAN      NOT NULL,
    settings    TEXT         NOT NULL,
    CONSTRAINT pk_type_visibilities PRIMARY KEY (id)
);

CREATE TABLE type_onboardings
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    label       VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order  INT          NOT NULL,
    is_active   BOOLEAN      NOT NULL,
    settings    TEXT         NOT NULL,
    orientation VARCHAR(255) NOT NULL,
    CONSTRAINT pk_type_onboardings PRIMARY KEY (id)
);

CREATE TABLE type_publisher_scopes
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    label       VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order  INT          NOT NULL,
    is_active   BOOLEAN      NOT NULL,
    settings    TEXT         NOT NULL,
    CONSTRAINT pk_type_publisher_scopes PRIMARY KEY (id)
);

CREATE TABLE type_schema_scopes
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    label       VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order  INT          NOT NULL,
    is_active   BOOLEAN      NOT NULL,
    settings    TEXT         NOT NULL,
    CONSTRAINT pk_type_schema_scopes PRIMARY KEY (id)
);

CREATE TABLE type_schemas
(
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    name                  VARCHAR(50)  NOT NULL,
    label                 VARCHAR(100) NOT NULL,
    description           TEXT,
    sort_order            INT          NOT NULL,
    is_active             BOOLEAN      NOT NULL,
    settings              TEXT         NOT NULL,
    type_schema_scopes_id BIGINT       NOT NULL,
    CONSTRAINT pk_type_schemas PRIMARY KEY (id),
    CONSTRAINT uk_type_schemas_scope_name UNIQUE (type_schema_scopes_id, name),
    CONSTRAINT fk_type_schemas_scope FOREIGN KEY (type_schema_scopes_id) REFERENCES type_schema_scopes (id)
);

CREATE TABLE type_sharing_statuses
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    label       VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order  INT          NOT NULL,
    is_active   BOOLEAN      NOT NULL,
    settings    TEXT         NOT NULL,
    CONSTRAINT pk_type_sharing_statuses PRIMARY KEY (id)
);


-- Current schema statements sourced from V5__standardize_advanced_catalogs.sql
ALTER TABLE type_features
DROP
FOREIGN KEY fk_type_features_scope,
DROP INDEX uk_type_features_scope_name,
DROP
COLUMN type_feature_scopes_id,
    DROP
COLUMN available;

ALTER TABLE type_schemas
DROP
FOREIGN KEY fk_type_schemas_scope,
DROP INDEX uk_type_schemas_scope_name,
DROP
COLUMN type_schema_scopes_id;

ALTER TABLE type_onboardings
DROP
COLUMN orientation;


-- Current schema statements sourced from V6__catalog_code_primary_key.sql
ALTER TABLE workspaces
DROP
CHECK ck_workspaces_workspace_type;

ALTER TABLE type_workspaces
    MODIFY COLUMN id BIGINT NOT NULL;
ALTER TABLE type_workspaces
DROP
PRIMARY KEY;
ALTER TABLE type_workspaces
DROP
COLUMN id,
    RENAME COLUMN name TO code;
ALTER TABLE type_workspaces
    ADD CONSTRAINT pk_type_workspaces PRIMARY KEY (code);

ALTER TABLE type_application_scopes
    MODIFY COLUMN id BIGINT NOT NULL;
ALTER TABLE type_application_scopes
DROP
PRIMARY KEY;
ALTER TABLE type_application_scopes
DROP
COLUMN id,
    RENAME COLUMN name TO code;
ALTER TABLE type_application_scopes
    ADD CONSTRAINT pk_type_application_scopes PRIMARY KEY (code);

ALTER TABLE type_authorizations
    MODIFY COLUMN id BIGINT NOT NULL;
ALTER TABLE type_authorizations
DROP
PRIMARY KEY;
ALTER TABLE type_authorizations
DROP
COLUMN id,
    RENAME COLUMN name TO code;
ALTER TABLE type_authorizations
    ADD CONSTRAINT pk_type_authorizations PRIMARY KEY (code);

ALTER TABLE type_feature_scopes
    MODIFY COLUMN id BIGINT NOT NULL;
ALTER TABLE type_feature_scopes
DROP
PRIMARY KEY;
ALTER TABLE type_feature_scopes
DROP
COLUMN id,
    RENAME COLUMN name TO code;
ALTER TABLE type_feature_scopes
    ADD CONSTRAINT pk_type_feature_scopes PRIMARY KEY (code);

ALTER TABLE type_features
    MODIFY COLUMN id BIGINT NOT NULL;
ALTER TABLE type_features
DROP
PRIMARY KEY;
ALTER TABLE type_features
DROP
COLUMN id,
    RENAME COLUMN name TO code;
ALTER TABLE type_features
    ADD CONSTRAINT pk_type_features PRIMARY KEY (code);

ALTER TABLE type_infrastructures
    MODIFY COLUMN id BIGINT NOT NULL;
ALTER TABLE type_infrastructures
DROP
PRIMARY KEY;
ALTER TABLE type_infrastructures
DROP
COLUMN id,
    RENAME COLUMN name TO code;
ALTER TABLE type_infrastructures
    ADD CONSTRAINT pk_type_infrastructures PRIMARY KEY (code);

ALTER TABLE type_languages
    MODIFY COLUMN id BIGINT NOT NULL;
ALTER TABLE type_languages
DROP
PRIMARY KEY;
ALTER TABLE type_languages
DROP
COLUMN id,
    RENAME COLUMN name TO code;
ALTER TABLE type_languages
    ADD CONSTRAINT pk_type_languages PRIMARY KEY (code);

ALTER TABLE type_life_cycle
    MODIFY COLUMN id BIGINT NOT NULL;
ALTER TABLE type_life_cycle
DROP
PRIMARY KEY;
ALTER TABLE type_life_cycle
DROP
COLUMN id,
    RENAME COLUMN name TO code;
ALTER TABLE type_life_cycle
    ADD CONSTRAINT pk_type_life_cycle PRIMARY KEY (code);

ALTER TABLE type_tag_origins
    MODIFY COLUMN id BIGINT NOT NULL;
ALTER TABLE type_tag_origins
DROP
PRIMARY KEY;
ALTER TABLE type_tag_origins
DROP
COLUMN id,
    RENAME COLUMN name TO code;
ALTER TABLE type_tag_origins
    ADD CONSTRAINT pk_type_tag_origins PRIMARY KEY (code);

ALTER TABLE type_visibilities
    MODIFY COLUMN id BIGINT NOT NULL;
ALTER TABLE type_visibilities
DROP
PRIMARY KEY;
ALTER TABLE type_visibilities
DROP
COLUMN id,
    RENAME COLUMN name TO code;
ALTER TABLE type_visibilities
    ADD CONSTRAINT pk_type_visibilities PRIMARY KEY (code);

ALTER TABLE type_onboardings
    MODIFY COLUMN id BIGINT NOT NULL;
ALTER TABLE type_onboardings
DROP
PRIMARY KEY;
ALTER TABLE type_onboardings
DROP
COLUMN id,
    RENAME COLUMN name TO code;
ALTER TABLE type_onboardings
    ADD CONSTRAINT pk_type_onboardings PRIMARY KEY (code);

ALTER TABLE type_publisher_scopes
    MODIFY COLUMN id BIGINT NOT NULL;
ALTER TABLE type_publisher_scopes
DROP
PRIMARY KEY;
ALTER TABLE type_publisher_scopes
DROP
COLUMN id,
    RENAME COLUMN name TO code;
ALTER TABLE type_publisher_scopes
    ADD CONSTRAINT pk_type_publisher_scopes PRIMARY KEY (code);

ALTER TABLE type_schema_scopes
    MODIFY COLUMN id BIGINT NOT NULL;
ALTER TABLE type_schema_scopes
DROP
PRIMARY KEY;
ALTER TABLE type_schema_scopes
DROP
COLUMN id,
    RENAME COLUMN name TO code;
ALTER TABLE type_schema_scopes
    ADD CONSTRAINT pk_type_schema_scopes PRIMARY KEY (code);

ALTER TABLE type_schemas
    MODIFY COLUMN id BIGINT NOT NULL;
ALTER TABLE type_schemas
DROP
PRIMARY KEY;
ALTER TABLE type_schemas
DROP
COLUMN id,
    RENAME COLUMN name TO code;
ALTER TABLE type_schemas
    ADD CONSTRAINT pk_type_schemas PRIMARY KEY (code);

ALTER TABLE type_sharing_statuses
    MODIFY COLUMN id BIGINT NOT NULL;
ALTER TABLE type_sharing_statuses
DROP
PRIMARY KEY;
ALTER TABLE type_sharing_statuses
DROP
COLUMN id,
    RENAME COLUMN name TO code;
ALTER TABLE type_sharing_statuses
    ADD CONSTRAINT pk_type_sharing_statuses PRIMARY KEY (code);

INSERT
IGNORE INTO type_workspaces
    (code, label, description, sort_order, is_active, settings)
VALUES
    ('ADMIN', 'Admin', 'Administrative workspace', 1, true, '{}'),
    ('MANAGER', 'Manager', 'Management workspace', 2, true, '{}'),
    ('CATALOG', 'Catalog', 'Catalog workspace', 3, true, '{}');

ALTER TABLE workspaces
    RENAME COLUMN workspace_type TO workspace_type_code;

ALTER TABLE workspaces
    MODIFY COLUMN workspace_type_code VARCHAR (50) NOT NULL,
    ADD CONSTRAINT fk_workspaces_workspace_type
    FOREIGN KEY (workspace_type_code) REFERENCES type_workspaces(code);


-- Current schema statements sourced from V7__workspace_lifecycle_catalog_code.sql
ALTER TABLE workspaces
DROP
CHECK ck_workspaces_lifecycle;

INSERT
IGNORE INTO type_life_cycle
    (code, label, description, sort_order, is_active, settings)
VALUES
    ('ACTIVE', 'Active', 'Active lifecycle state', 1, true, '{}'),
    ('INACTIVE', 'Inactive', 'Inactive lifecycle state', 2, true, '{}'),
    ('PENDING_DELETION', 'Pending deletion', 'Pending physical deletion', 3, true, '{}');

ALTER TABLE workspaces
    RENAME COLUMN lifecycle TO lifecycle_code;

ALTER TABLE workspaces
    MODIFY COLUMN lifecycle_code VARCHAR (50) NOT NULL,
    ADD CONSTRAINT fk_workspaces_lifecycle
    FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code);


-- Current schema statements sourced from V8__workspace_approver_uniqueness.sql
ALTER TABLE workspace_approvers
    ADD CONSTRAINT uk_workspace_approvers_functional
        UNIQUE (workspace_id, functional),
    ADD CONSTRAINT uk_workspace_approvers_email
        UNIQUE (workspace_id, email);


-- Current schema statements sourced from V9__create_message_management.sql
CREATE TABLE type_services
(
    code        VARCHAR(50)  NOT NULL,
    label       VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order  INT          NOT NULL,
    is_active   BOOLEAN      NOT NULL,
    settings    TEXT         NOT NULL,
    CONSTRAINT pk_type_services PRIMARY KEY (code)
);

INSERT INTO type_services (code, label, description, sort_order, is_active, settings)
VALUES ('workspace-service',
        'Workspace Service',
        'Workspace and platform administration service',
        1,
        true,
        '{}');

CREATE TABLE messages
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    version        BIGINT       NOT NULL DEFAULT 0,
    identifier     VARCHAR(36)  NOT NULL,
    service_code   VARCHAR(50)  NOT NULL,
    message_key    VARCHAR(255) NOT NULL,
    code           VARCHAR(50)  NOT NULL,
    http_status    INT          NOT NULL,
    lifecycle_code VARCHAR(50)  NOT NULL,
    observation    VARCHAR(500) NULL,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    CONSTRAINT pk_messages PRIMARY KEY (id),
    CONSTRAINT uk_messages_identifier UNIQUE (identifier),
    CONSTRAINT uk_messages_service_message_key UNIQUE (service_code, message_key),
    CONSTRAINT uk_messages_service_code UNIQUE (service_code, code),
    CONSTRAINT fk_messages_service
        FOREIGN KEY (service_code) REFERENCES type_services (code),
    CONSTRAINT fk_messages_lifecycle
        FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle (code),
    CONSTRAINT ck_messages_http_status
        CHECK (http_status BETWEEN 100 AND 599)
);

CREATE TABLE message_translations
(
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    version        BIGINT        NOT NULL DEFAULT 0,
    identifier     VARCHAR(36)   NOT NULL,
    message_id     BIGINT        NOT NULL,
    locale         VARCHAR(10)   NOT NULL,
    title          VARCHAR(150)  NOT NULL,
    detail         VARCHAR(1000) NOT NULL,
    suggestion     VARCHAR(1000) NOT NULL,
    lifecycle_code VARCHAR(50)   NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    CONSTRAINT pk_message_translations PRIMARY KEY (id),
    CONSTRAINT uk_message_translations_identifier UNIQUE (identifier),
    CONSTRAINT uk_message_translations_message_locale UNIQUE (message_id, locale),
    CONSTRAINT fk_message_translations_message
        FOREIGN KEY (message_id) REFERENCES messages (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_message_translations_lifecycle
        FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle (code)
);

CREATE INDEX idx_messages_service
    ON messages (service_code);

CREATE INDEX idx_messages_lifecycle
    ON messages (lifecycle_code);

CREATE INDEX idx_message_translations_lifecycle
    ON message_translations (lifecycle_code);

CREATE
OR REPLACE VIEW vw_platform_messages AS
SELECT m.service_code AS service,
       m.message_key  AS message_key,
       m.code         AS code,
       m.http_status  AS http_status,
       mt.locale      AS locale,
       mt.title       AS title,
       mt.detail      AS detail,
       mt.suggestion  AS suggestion
FROM messages m
         JOIN type_services s
              ON s.code = m.service_code
         JOIN message_translations mt
              ON mt.message_id = m.id
WHERE s.is_active = TRUE
  AND m.lifecycle_code = 'ACTIVE'
  AND mt.lifecycle_code = 'ACTIVE';


-- Current schema statements sourced from V10__quarantined_lifecycle.sql
INSERT
IGNORE INTO type_life_cycle
    (code, label, description, sort_order, is_active, settings)
VALUES
    ('QUARANTINED', 'Quarantined', 'Quarantined lifecycle state', 3, true, '{}');

UPDATE workspaces
SET lifecycle_code = 'QUARANTINED'
WHERE lifecycle_code = 'PENDING_DELETION';

UPDATE messages
SET lifecycle_code = 'QUARANTINED'
WHERE lifecycle_code = 'PENDING_DELETION';

UPDATE message_translations
SET lifecycle_code = 'QUARANTINED'
WHERE lifecycle_code = 'PENDING_DELETION';

DELETE
FROM type_life_cycle
WHERE code = 'PENDING_DELETION';


-- Current schema statements sourced from V11__normalize_message_locale_and_runtime_view.sql
ALTER TABLE message_translations
    MODIFY COLUMN locale VARCHAR (35) NOT NULL;

UPDATE message_translations
SET locale = REPLACE(locale, '_', '-')
WHERE locale LIKE '%\_%' ESCAPE '\\';

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
         JOIN type_services s
              ON s.code = m.service_code
         JOIN message_translations mt
              ON mt.message_id = m.id
WHERE s.is_active = TRUE
  AND m.lifecycle_code = 'ACTIVE'
  AND mt.lifecycle_code = 'ACTIVE';


-- Current schema statements sourced from V12__service_feature_runtime_configuration.sql
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
);

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
);

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
);

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
                   ON BINARY fallback_service.code = BINARY 'workspace-service'
         LEFT JOIN platform_services owner_service
                   ON JSON_VALID(f.settings)
                       AND BINARY owner_service.code = BINARY JSON_UNQUOTE(JSON_EXTRACT(f.settings, '$.service'));

-- Promote both historical single-scope and array-scope representations to the explicit N:N relation.
INSERT
IGNORE INTO platform_feature_scopes (feature_id, feature_scope_code)
SELECT pf.id, fs.code
FROM type_features legacy
         JOIN platform_features pf ON pf.code = legacy.code
         JOIN type_feature_scopes fs
              ON BINARY fs.code = BINARY JSON_UNQUOTE(JSON_EXTRACT(legacy.settings, '$.scopes'))
WHERE JSON_VALID(legacy.settings)
  AND JSON_TYPE(JSON_EXTRACT(legacy.settings, '$.scopes')) = 'STRING';

INSERT
IGNORE INTO platform_feature_scopes (feature_id, feature_scope_code)
SELECT pf.id, fs.code
FROM type_features legacy
         JOIN platform_features pf ON pf.code = legacy.code
         JOIN JSON_TABLE(
        CASE
            WHEN JSON_VALID(legacy.settings)
                AND JSON_TYPE(JSON_EXTRACT(legacy.settings, '$.scopes')) = 'ARRAY'
                THEN JSON_EXTRACT(legacy.settings, '$.scopes')
            ELSE JSON_ARRAY()
            END,
        '$[*]' COLUMNS(scope_code VARCHAR(50) PATH '$')
              ) legacy_scope
         JOIN type_feature_scopes fs ON BINARY fs.code = BINARY legacy_scope.scope_code;

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
              ON s.code = m.service_code
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


-- Current schema statements sourced from V13__feature_context_platform.sql
CREATE TABLE platform_feature_contexts
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
    CONSTRAINT pk_platform_feature_contexts PRIMARY KEY (id),
    CONSTRAINT uk_platform_feature_contexts_identifier UNIQUE (identifier),
    CONSTRAINT uk_platform_feature_contexts_code UNIQUE (code),
    CONSTRAINT uk_platform_feature_contexts_name UNIQUE (name),
    CONSTRAINT ck_platform_feature_contexts_code
        CHECK (code REGEXP '^[A-Z][A-Z0-9_]*$'
) ,
    CONSTRAINT fk_platform_feature_contexts_lifecycle
        FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code)
);

INSERT INTO platform_feature_contexts
(version, identifier, code, name, description, lifecycle_code, created_at, updated_at)
SELECT 0,
       UUID(),
       UPPER(REPLACE(code, '-', '_')),
       label,
       description,
       CASE WHEN is_active THEN 'ACTIVE' ELSE 'INACTIVE' END,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM type_feature_scopes;

CREATE TABLE platform_feature_context_relations
(
    feature_id         BIGINT NOT NULL,
    feature_context_id BIGINT NOT NULL,
    CONSTRAINT pk_platform_feature_context_relations PRIMARY KEY (feature_id, feature_context_id),
    CONSTRAINT fk_platform_feature_context_relations_feature
        FOREIGN KEY (feature_id) REFERENCES platform_features (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_platform_feature_context_relations_context
        FOREIGN KEY (feature_context_id) REFERENCES platform_feature_contexts (id)
);

INSERT INTO platform_feature_context_relations (feature_id, feature_context_id)
SELECT relation.feature_id,
       context.id
FROM platform_feature_scopes relation
         JOIN platform_feature_contexts context
              ON context.code = UPPER(REPLACE(relation.feature_scope_code, '-', '_'));

ALTER TABLE messages
DROP
FOREIGN KEY fk_messages_platform_service;

UPDATE platform_services
SET code = UPPER(REPLACE(code, '-', '_'));

UPDATE platform_features
SET code = UPPER(REPLACE(code, '-', '_'));

UPDATE messages
SET service_code = UPPER(REPLACE(service_code, '-', '_'));

ALTER TABLE platform_services
    ADD CONSTRAINT uk_platform_services_name UNIQUE (name),
    ADD CONSTRAINT ck_platform_services_code
        CHECK (code REGEXP '^[A-Z][A-Z0-9_]*$');

ALTER TABLE platform_features
    ADD CONSTRAINT uk_platform_features_name UNIQUE (name),
    ADD CONSTRAINT ck_platform_features_code
        CHECK (code REGEXP '^[A-Z][A-Z0-9_]*$');

ALTER TABLE messages
    ADD CONSTRAINT fk_messages_platform_service
        FOREIGN KEY (service_code) REFERENCES platform_services (code);

DROP TABLE platform_feature_scopes;
DROP TABLE type_feature_scopes;


-- Current schema statements sourced from V14__resource_scope_type.sql
CREATE TABLE type_resource_scopes
(
    code        VARCHAR(50)  NOT NULL,
    label       VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order  INT          NOT NULL,
    is_active   BOOLEAN      NOT NULL,
    settings    TEXT         NOT NULL,
    CONSTRAINT pk_type_resource_scopes PRIMARY KEY (code)
);

INSERT
IGNORE INTO type_resource_scopes
    (code, label, description, sort_order, is_active, settings)
SELECT code,
       label,
       description,
       sort_order,
       is_active,
       settings
FROM type_publisher_scopes;

INSERT
IGNORE INTO type_resource_scopes
    (code, label, description, sort_order, is_active, settings)
SELECT code,
       label,
       description,
       sort_order,
       is_active,
       settings
FROM type_schemas;

DROP TABLE type_publisher_scopes;
DROP TABLE type_schemas;


-- Current schema statements sourced from V15__platform_kebab_case_codes.sql
ALTER TABLE messages
DROP
FOREIGN KEY fk_messages_platform_service;

ALTER TABLE platform_services
DROP
CHECK ck_platform_services_code;

ALTER TABLE platform_features
DROP
CHECK ck_platform_features_code;

ALTER TABLE platform_feature_contexts
DROP
CHECK ck_platform_feature_contexts_code;

UPDATE platform_services
SET code = LOWER(REPLACE(code, '_', '-'));

UPDATE platform_features
SET code = LOWER(REPLACE(code, '_', '-'));

UPDATE platform_feature_contexts
SET code = LOWER(REPLACE(code, '_', '-'));

UPDATE messages
SET service_code = LOWER(REPLACE(service_code, '_', '-'));

ALTER TABLE platform_services
    ADD CONSTRAINT ck_platform_services_code
        CHECK (code REGEXP '^[a-z][a-z0-9]*(-[a-z0-9]+)*$');

ALTER TABLE platform_features
    ADD CONSTRAINT ck_platform_features_code
        CHECK (code REGEXP '^[a-z][a-z0-9]*(-[a-z0-9]+)*$');

ALTER TABLE platform_feature_contexts
    ADD CONSTRAINT ck_platform_feature_contexts_code
        CHECK (code REGEXP '^[a-z][a-z0-9]*(-[a-z0-9]+)*$');

ALTER TABLE messages
    ADD CONSTRAINT fk_messages_platform_service
        FOREIGN KEY (service_code) REFERENCES platform_services (code);


-- Current schema statements sourced from V16__message_service_relation.sql
DROP VIEW IF EXISTS vw_platform_messages;

ALTER TABLE messages
    ADD COLUMN service_id BIGINT NULL AFTER identifier;

UPDATE messages m
    JOIN platform_services s
ON s.code = m.service_code
    SET m.service_id = s.id;

ALTER TABLE messages
    MODIFY COLUMN service_id BIGINT NOT NULL;

ALTER TABLE messages
DROP
FOREIGN KEY fk_messages_platform_service,
DROP INDEX uk_messages_service_message_key,
DROP INDEX uk_messages_service_code,
DROP INDEX idx_messages_service,
DROP
COLUMN service_code;

ALTER TABLE messages
    ADD CONSTRAINT uk_messages_service_message_key
        UNIQUE (service_id, message_key),
    ADD CONSTRAINT uk_messages_service_message_code
        UNIQUE (service_id, code),
    ADD CONSTRAINT fk_messages_platform_service
        FOREIGN KEY (service_id) REFERENCES platform_services(id);

CREATE INDEX idx_messages_service
    ON messages (service_id);

CREATE
OR REPLACE VIEW vw_platform_messages AS
SELECT CONCAT(s.code, '.', m.message_key) AS message_key,
       m.code                             AS code,
       m.http_status                      AS http_status,
       mt.locale                          AS locale,
       mt.title                           AS title,
       mt.detail                          AS message,
       mt.suggestion                      AS solution
FROM messages m
         JOIN platform_services s
              ON s.id = m.service_id
         JOIN message_translations mt
              ON mt.message_id = m.id
WHERE s.lifecycle_code = 'ACTIVE'
  AND m.lifecycle_code = 'ACTIVE'
  AND mt.lifecycle_code = 'ACTIVE';


-- Current schema statements sourced from V17__schema_v2_foundation.sql
INSERT
IGNORE INTO type_life_cycle
    (code, label, description, sort_order, is_active, settings)
VALUES
    ('ACTIVE', 'Active', 'Active lifecycle state', 1, true, '{}'),
    ('INACTIVE', 'Inactive', 'Inactive lifecycle state', 2, true, '{}'),
    ('QUARANTINED', 'Quarantined', 'Quarantined lifecycle state', 3, true, '{}');

INSERT
IGNORE INTO type_schema_scopes
    (code, label, description, sort_order, is_active, settings)
VALUES
    ('PLATFORM', 'Platform', 'Schema owned by the platform', 1, true, '{}'),
    ('WORKSPACE', 'Workspace', 'Schema owned by a workspace', 2, true, '{}');

CREATE TABLE type_schema_types
(
    code        VARCHAR(50)  NOT NULL,
    label       VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order  INT          NOT NULL,
    is_active   BOOLEAN      NOT NULL,
    settings    TEXT         NOT NULL,
    CONSTRAINT pk_type_schema_types PRIMARY KEY (code),
    CONSTRAINT ck_type_schema_types_code CHECK (code REGEXP '^[A-Z][A-Z0-9_]{0,49}$'
)
    );

CREATE TABLE schema_definitions
(
    id                        BIGINT       NOT NULL AUTO_INCREMENT,
    version                   BIGINT       NOT NULL DEFAULT 0,
    identifier                VARCHAR(36)  NOT NULL,
    schema_type_code          VARCHAR(50)  NOT NULL,
    scope_code                VARCHAR(50)  NOT NULL,
    workspace_id              BIGINT,
    owner_key                 BIGINT GENERATED ALWAYS AS (COALESCE(workspace_id, 0)) STORED,
    platform_schema_type_code VARCHAR(50)
        GENERATED ALWAYS AS (
            CASE WHEN scope_code = 'PLATFORM' THEN schema_type_code ELSE NULL END
            ) STORED,
    code                      VARCHAR(50)  NOT NULL,
    name                      VARCHAR(100) NOT NULL,
    description               VARCHAR(500),
    lifecycle_code            VARCHAR(50)  NOT NULL,
    created_at                TIMESTAMP    NOT NULL,
    updated_at                TIMESTAMP    NOT NULL,
    CONSTRAINT pk_schema_definitions PRIMARY KEY (id),
    CONSTRAINT uk_schema_definitions_identifier UNIQUE (identifier),
    CONSTRAINT uk_schema_definitions_scope_owner_type_code
        UNIQUE (scope_code, owner_key, schema_type_code, code),
    CONSTRAINT uk_schema_definitions_platform_type
        UNIQUE (platform_schema_type_code),
    CONSTRAINT ck_schema_definitions_code CHECK (code REGEXP '^[a-z][a-z0-9]*(-[a-z0-9]+)*$'
) ,
    CONSTRAINT ck_schema_definitions_owner
        CHECK (
            (scope_code = 'PLATFORM' AND workspace_id IS NULL)
            OR
            (scope_code = 'WORKSPACE' AND workspace_id IS NOT NULL)
        ),
    CONSTRAINT fk_schema_definitions_type
        FOREIGN KEY (schema_type_code) REFERENCES type_schema_types(code),
    CONSTRAINT fk_schema_definitions_scope
        FOREIGN KEY (scope_code) REFERENCES type_schema_scopes(code),
    CONSTRAINT fk_schema_definitions_lifecycle
        FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code),
    CONSTRAINT fk_schema_definitions_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspaces(id)
);

CREATE TABLE schema_versions
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    identifier     VARCHAR(36)  NOT NULL,
    schema_id      BIGINT       NOT NULL,
    schema_version INT          NOT NULL,
    version_name   VARCHAR(100) NOT NULL,
    definition     JSON         NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    created_at     TIMESTAMP    NOT NULL,
    CONSTRAINT pk_schema_versions PRIMARY KEY (id),
    CONSTRAINT uk_schema_versions_identifier UNIQUE (identifier),
    CONSTRAINT uk_schema_versions_schema_version UNIQUE (schema_id, schema_version),
    CONSTRAINT ck_schema_versions_status CHECK (status IN ('DRAFT', 'PUBLISHED')),
    CONSTRAINT fk_schema_versions_schema
        FOREIGN KEY (schema_id) REFERENCES schema_definitions (id)
);

CREATE INDEX idx_schema_definitions_type_scope_owner
    ON schema_definitions (schema_type_code, scope_code, workspace_id);

CREATE INDEX idx_schema_versions_resolution
    ON schema_versions (schema_id, status, schema_version);

INSERT INTO type_schema_types
    (code, label, description, sort_order, is_active, settings)
VALUES ('DEFAULT', 'Default', 'Fallback schema type for dynamic settings', 1, true, '{}');

INSERT INTO schema_definitions
(version, identifier, schema_type_code, scope_code, workspace_id, code, name, description,
 lifecycle_code, created_at, updated_at)
VALUES (0, UUID(), 'DEFAULT', 'PLATFORM', NULL, 'default', 'Default',
        'Permissive fallback schema for platform settings',
        'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO schema_versions
(identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(),
       s.id,
       1,
       'v1',
       '{"$schema":"https://json-schema.org/draft/2020-12/schema","title":"Default Dynamic Schema","type":"object","additionalProperties":true}',
       'PUBLISHED',
       CURRENT_TIMESTAMP
FROM schema_definitions s
WHERE s.schema_type_code = 'DEFAULT'
  AND s.scope_code = 'PLATFORM'
  AND s.code = 'default';


-- Current schema statements sourced from V18__schema_version_status_v3.sql
CREATE TABLE type_schema_version_status
(
    code        VARCHAR(20)  NOT NULL,
    label       VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order  INT          NOT NULL,
    is_active   BOOLEAN      NOT NULL,
    settings    TEXT         NOT NULL,
    CONSTRAINT pk_type_schema_version_status PRIMARY KEY (code),
    CONSTRAINT ck_type_schema_version_status_code
        CHECK (code REGEXP '^[A-Z][A-Z0-9_]{0,19}$'
)
    );

INSERT INTO type_schema_version_status
    (code, label, description, sort_order, is_active, settings)
VALUES ('DRAFT', 'Draft', 'Schema version under edition and not available for consumption', 1, true, '{}'),
       ('PUBLISHED', 'Published', 'Published immutable schema version available for consumption', 2, true, '{}');

ALTER TABLE schema_versions
    ADD COLUMN draft_schema_id BIGINT
        GENERATED ALWAYS AS (
            CASE WHEN status = 'DRAFT' THEN schema_id ELSE NULL END
            ) STORED,
    ADD CONSTRAINT uk_schema_versions_single_draft UNIQUE (draft_schema_id),
    ADD CONSTRAINT fk_schema_versions_status
        FOREIGN KEY (status) REFERENCES type_schema_version_status(code);


-- Current schema statements sourced from V19__microservice_and_retire_unused_catalogs.sql
-- Upgrade existing installations without recreating microservices, features or messages.
DROP VIEW IF EXISTS vw_platform_messages;
DROP VIEW IF EXISTS vw_feature_runtime_config;

ALTER TABLE platform_features DROP FOREIGN KEY fk_platform_features_service;
ALTER TABLE messages DROP FOREIGN KEY fk_messages_platform_service;

RENAME
TABLE platform_services TO platform_microservices;

ALTER TABLE platform_microservices
    RENAME INDEX uk_platform_services_identifier TO uk_platform_microservices_identifier,
    RENAME INDEX uk_platform_services_code TO uk_platform_microservices_code,
    RENAME INDEX uk_platform_services_name TO uk_platform_microservices_name,
DROP
CHECK ck_platform_services_code,
    ADD CONSTRAINT ck_platform_microservices_code
        CHECK (code REGEXP '^[a-z][a-z0-9]*(-[a-z0-9]+)*$');

ALTER TABLE platform_features
    CHANGE COLUMN service_id microservice_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_platform_features_microservice
    FOREIGN KEY (microservice_id) REFERENCES platform_microservices(id);

ALTER TABLE messages
DROP INDEX uk_messages_service_message_key,
DROP INDEX uk_messages_service_message_code,
DROP INDEX idx_messages_service,
    CHANGE COLUMN service_id microservice_id BIGINT NOT NULL,
    ADD CONSTRAINT uk_messages_microservice_message_key UNIQUE (microservice_id, message_key),
    ADD CONSTRAINT uk_messages_microservice_message_code UNIQUE (microservice_id, code),
    ADD CONSTRAINT fk_messages_platform_microservice
        FOREIGN KEY (microservice_id) REFERENCES platform_microservices(id);

CREATE INDEX idx_messages_microservice ON messages (microservice_id);

CREATE VIEW vw_platform_messages AS
SELECT CONCAT(s.code, '.', m.message_key) AS message_key,
       m.code                             AS code,
       m.http_status                      AS http_status,
       mt.locale                          AS locale,
       mt.title                           AS title,
       mt.detail                          AS message,
       mt.suggestion                      AS solution
FROM messages m
         JOIN platform_microservices s ON s.id = m.microservice_id
         JOIN message_translations mt ON mt.message_id = m.id
WHERE s.lifecycle_code = 'ACTIVE'
  AND m.lifecycle_code = 'ACTIVE'
  AND mt.lifecycle_code = 'ACTIVE';

CREATE VIEW vw_feature_runtime_config AS
SELECT f.code                                                                                 AS feature_code,
       f.name                                                                                 AS feature_label,
       s.code                                                                                 AS microservice_code,
       s.name                                                                                 AS microservice_label,
       JSON_EXTRACT(f.settings, '$.quarantine.enabled') = true                                AS quarantine_enabled,
       CAST(JSON_UNQUOTE(JSON_EXTRACT(f.settings, '$.quarantine.retentionDays')) AS UNSIGNED) AS quarantine_retention_days,
       JSON_EXTRACT(f.settings, '$.quarantine.restoreAllowed') = true                         AS quarantine_restore_allowed,
       JSON_EXTRACT(f.settings, '$.audit.enabled') = true                                     AS audit_enabled,
       JSON_EXTRACT(f.settings, '$.audit.snapshotOnPurge') = true                             AS audit_snapshot_on_purge,
       JSON_EXTRACT(f.settings, '$.purge.enabled') = true                                     AS purge_enabled,
       f.lifecycle_code = 'ACTIVE'                                                            AS feature_active,
       s.lifecycle_code = 'ACTIVE'                                                            AS microservice_active
FROM platform_features f
         JOIN platform_microservices s ON s.id = f.microservice_id;

-- Application settings will own these concepts when Application is introduced.
DROP TABLE type_languages;
DROP TABLE type_infrastructures;


-- Current schema statements sourced from V20__schema_type_domain.sql
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
SET identifier = UUID(),
    lifecycle_code = CASE WHEN is_active THEN 'ACTIVE' ELSE 'INACTIVE' END;

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
    ADD CONSTRAINT fk_schema_types_lifecycle
        FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code);

CREATE TABLE schema_type_scopes
(
    schema_type_id BIGINT      NOT NULL,
    scope_code     VARCHAR(50) NOT NULL,
    CONSTRAINT pk_schema_type_scopes PRIMARY KEY (schema_type_id, scope_code),
    CONSTRAINT fk_schema_type_scopes_type
        FOREIGN KEY (schema_type_id) REFERENCES schema_types(id),
    CONSTRAINT fk_schema_type_scopes_scope
        FOREIGN KEY (scope_code) REFERENCES type_schema_scopes(code)
);

INSERT INTO schema_type_scopes (schema_type_id, scope_code)
SELECT st.id, ss.code
FROM schema_types st
CROSS JOIN type_schema_scopes ss
WHERE ss.code IN ('PLATFORM', 'WORKSPACE');

ALTER TABLE schema_definitions
    ADD CONSTRAINT fk_schema_definitions_type
        FOREIGN KEY (schema_type_code) REFERENCES schema_types(code);

ALTER TABLE schema_types
    DROP INDEX uk_schema_types_id;


-- Current schema statements sourced from V21__schema_type_default_scope.sql
UPDATE schema_types
SET lifecycle_code = 'ACTIVE'
WHERE code = 'DEFAULT';

UPDATE schema_definitions
SET lifecycle_code = 'ACTIVE'
WHERE schema_type_code = 'DEFAULT'
  AND scope_code = 'PLATFORM';

DELETE sts
FROM schema_type_scopes sts
JOIN schema_types st ON st.id = sts.schema_type_id
WHERE st.code = 'DEFAULT'
  AND sts.scope_code <> 'PLATFORM';

INSERT INTO schema_type_scopes (schema_type_id, scope_code)
SELECT st.id, 'PLATFORM'
FROM schema_types st
WHERE st.code = 'DEFAULT'
  AND NOT EXISTS (
      SELECT 1
      FROM schema_type_scopes sts
      WHERE sts.schema_type_id = st.id
        AND sts.scope_code = 'PLATFORM'
  );


-- Current schema statements sourced from V22__schema_type_single_scope.sql
CREATE TEMPORARY TABLE schema_type_scope_keep AS
SELECT st.id AS schema_type_id,
       CASE
           WHEN EXISTS (
               SELECT 1
               FROM schema_definitions sd
               WHERE sd.schema_type_code = st.code
                 AND sd.scope_code = 'WORKSPACE'
           )
           AND NOT EXISTS (
               SELECT 1
               FROM schema_definitions sd
               WHERE sd.schema_type_code = st.code
                 AND sd.scope_code = 'PLATFORM'
           )
               THEN 'WORKSPACE'
           ELSE 'PLATFORM'
       END AS scope_code
FROM schema_types st;

DELETE FROM schema_type_scopes;

INSERT INTO schema_type_scopes (schema_type_id, scope_code)
SELECT schema_type_id, scope_code
FROM schema_type_scope_keep;

DROP TEMPORARY TABLE schema_type_scope_keep;

ALTER TABLE schema_type_scopes
    ADD CONSTRAINT uk_schema_type_scopes_type UNIQUE (schema_type_id);


-- Current schema statements sourced from V23__schema_type_scope_column.sql
ALTER TABLE schema_types
    ADD COLUMN scope_code VARCHAR(50) NULL;

UPDATE schema_types st
JOIN schema_type_scopes sts ON sts.schema_type_id = st.id
SET st.scope_code = sts.scope_code;

ALTER TABLE schema_types
    MODIFY COLUMN scope_code VARCHAR(50) NOT NULL,
    ADD CONSTRAINT fk_schema_types_scope
        FOREIGN KEY (scope_code) REFERENCES type_schema_scopes(code);

DROP TABLE schema_type_scopes;


-- Current schema statements sourced from V24__create_applications.sql
CREATE TABLE applications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL,
    workspace_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    alias VARCHAR(100) NOT NULL,
    acronym VARCHAR(20) NOT NULL,
    application_scope_code VARCHAR(50) NOT NULL,
    authorizer_group VARCHAR(255) NULL,
    settings TEXT NOT NULL,
    lifecycle_code VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_applications PRIMARY KEY (id),
    CONSTRAINT uk_applications_identifier UNIQUE (identifier),
    CONSTRAINT uk_applications_workspace_name UNIQUE (workspace_id, name),
    CONSTRAINT fk_applications_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
    CONSTRAINT fk_applications_scope FOREIGN KEY (application_scope_code) REFERENCES type_application_scopes(code),
    CONSTRAINT fk_applications_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code)
);

CREATE INDEX idx_applications_workspace_lifecycle ON applications(workspace_id, lifecycle_code);


-- Current schema statements sourced from V25__create_environments.sql
CREATE TABLE environment_types (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL UNIQUE,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(250) NOT NULL,
    root_allowed BOOLEAN NOT NULL,
    workspace_required BOOLEAN NOT NULL,
    lifecycle_code VARCHAR(50) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE TABLE environment_type_compatibilities (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    identifier VARCHAR(36) NOT NULL UNIQUE,
    parent_type_id BIGINT NOT NULL,
    child_type_id BIGINT NOT NULL,
    lifecycle_code VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_environment_compatibility UNIQUE (parent_type_id, child_type_id),
    CONSTRAINT fk_environment_compatibility_parent FOREIGN KEY (parent_type_id) REFERENCES environment_types(id),
    CONSTRAINT fk_environment_compatibility_child FOREIGN KEY (child_type_id) REFERENCES environment_types(id)
);

CREATE TABLE environments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL UNIQUE,
    workspace_id BIGINT NULL,
    environment_type_id BIGINT NOT NULL,
    parent_environment_id BIGINT NULL,
    name VARCHAR(100) NOT NULL,
    name_workspace_scope BIGINT GENERATED ALWAYS AS (COALESCE(workspace_id, 0)) STORED,
    name_parent_scope BIGINT GENERATED ALWAYS AS (COALESCE(parent_environment_id, 0)) STORED,
    description VARCHAR(250) NOT NULL,
    authorization_type_code VARCHAR(50) NOT NULL,
    authorizer_group VARCHAR(255) NULL,
    settings TEXT NOT NULL,
    sort_order INT NOT NULL,
    lifecycle_code VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_environments_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
    CONSTRAINT fk_environments_authorization FOREIGN KEY (authorization_type_code) REFERENCES type_authorizations(code),
    CONSTRAINT fk_environments_type FOREIGN KEY (environment_type_id) REFERENCES environment_types(id),
    CONSTRAINT fk_environments_parent FOREIGN KEY (parent_environment_id) REFERENCES environments(id),
    CONSTRAINT fk_environments_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code),
    CONSTRAINT uk_environments_sibling_name UNIQUE (name_workspace_scope, name_parent_scope, name),
    CONSTRAINT ck_environments_sort CHECK (sort_order >= 1)
);

CREATE INDEX idx_environments_scope_lifecycle ON environments (workspace_id, lifecycle_code, parent_environment_id, sort_order);
CREATE INDEX idx_environments_parent ON environments (parent_environment_id, lifecycle_code, sort_order);

INSERT INTO environment_types (identifier, code, name, description, root_allowed, workspace_required, lifecycle_code, display_order, created_at, updated_at) VALUES
    (UUID(), 'DEFAULT', 'Default', 'Global environment', true, false, 'ACTIVE', 1, NOW(6), NOW(6)),
    (UUID(), 'CUSTOM', 'Custom', 'Workspace environment', true, true, 'ACTIVE', 2, NOW(6), NOW(6)),
    (UUID(), 'SHARD', 'Shard', 'Workspace shard', false, true, 'ACTIVE', 3, NOW(6), NOW(6)),
    (UUID(), 'CELL', 'Cell', 'Workspace cell', false, true, 'ACTIVE', 4, NOW(6), NOW(6));

INSERT INTO environment_type_compatibilities (identifier, parent_type_id, child_type_id, lifecycle_code, created_at, updated_at)
SELECT UUID(), p.id, c.id, 'ACTIVE', NOW(6), NOW(6) FROM environment_types p JOIN environment_types c
WHERE (p.code = 'DEFAULT' AND c.code = 'SHARD')
   OR (p.code = 'CUSTOM' AND c.code = 'SHARD')
   OR (p.code = 'SHARD' AND c.code = 'CELL');


-- Current schema statements sourced from V26__create_publishers.sql
CREATE TABLE publishers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL,
    code VARCHAR(40) NOT NULL,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(500) NOT NULL,
    publisher_scope VARCHAR(20) NOT NULL,
    deprecated BOOLEAN NOT NULL DEFAULT FALSE,
    lifecycle_code VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_publishers PRIMARY KEY (id),
    CONSTRAINT uk_publishers_identifier UNIQUE (identifier),
    CONSTRAINT uk_publishers_code UNIQUE (code),
    CONSTRAINT fk_publishers_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code),
    CONSTRAINT ck_publishers_scope CHECK (publisher_scope IN ('WORKSPACE', 'APPLICATION')),
    CONSTRAINT ck_publishers_code CHECK (code REGEXP '^[A-Z][A-Z0-9_]{0,39}$')
);

INSERT INTO schema_types
    (version, identifier, code, name, description, lifecycle_code, scope_code, created_at, updated_at)
VALUES
    (0, UUID(), 'PUBLISHER_WEB_SOCKET', 'Web Socket publisher', 'Platform schema for Web Socket publishers', 'ACTIVE', 'PLATFORM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (0, UUID(), 'PUBLISHER_KAAS', 'KAAS publisher', 'Platform schema for KAAS publishers', 'ACTIVE', 'PLATFORM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (0, UUID(), 'PUBLISHER_APPCONFIG', 'AppConfig publisher', 'Platform schema for AppConfig publishers', 'ACTIVE', 'PLATFORM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


-- Current schema statements sourced from V27__publisher_scope_catalog_reference.sql
ALTER TABLE publishers
    MODIFY publisher_scope VARCHAR(50) NOT NULL,
    ADD CONSTRAINT fk_publishers_scope FOREIGN KEY (publisher_scope) REFERENCES type_resource_scopes(code);


-- Current schema statements sourced from V28__schema_configuration.sql
-- Preserve platform bindings before removing the legacy schema type directory.
CREATE TABLE schema_configuration (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL,
    resource_type VARCHAR(30) NOT NULL,
    resource_code VARCHAR(50) NOT NULL,
    schema_id BIGINT NOT NULL,
    lifecycle_code VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_schema_configuration PRIMARY KEY (id),
    CONSTRAINT uk_schema_configuration_identifier UNIQUE (identifier),
    CONSTRAINT uk_schema_configuration_resource UNIQUE (resource_type, resource_code),
    CONSTRAINT fk_schema_configuration_schema FOREIGN KEY (schema_id) REFERENCES schema_definitions(id),
    CONSTRAINT fk_schema_configuration_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code),
    CONSTRAINT ck_schema_configuration_resource_type CHECK
        (resource_type IN ('WORKSPACE', 'APPLICATION', 'ENVIRONMENT', 'PUBLISHER',
                           'FEATURE', 'MICROSERVICE', 'CATALOG'))
);

INSERT INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(),
       CASE
           WHEN sd.schema_type_code IN ('WORKSPACE', 'APPLICATION', 'ENVIRONMENT', 'PUBLISHER', 'FEATURE', 'MICROSERVICE')
               THEN sd.schema_type_code
           WHEN LEFT(sd.schema_type_code, 10) = 'PUBLISHER_' THEN 'PUBLISHER'
           WHEN LEFT(sd.schema_type_code, 8) = 'FEATURE_' THEN 'FEATURE'
           WHEN RIGHT(sd.schema_type_code, 5) = '_TYPE' THEN 'CATALOG'
       END,
       CASE
           WHEN LEFT(sd.schema_type_code, 10) = 'PUBLISHER_'
               THEN SUBSTRING(sd.schema_type_code, 11)
           WHEN LEFT(sd.schema_type_code, 8) = 'FEATURE_'
               THEN LOWER(REPLACE(SUBSTRING(sd.schema_type_code, 9), '_', '-'))
           WHEN RIGHT(sd.schema_type_code, 5) = '_TYPE'
               THEN LOWER(REPLACE(sd.schema_type_code, '_', '-'))
           WHEN sd.schema_type_code = 'ENVIRONMENT' THEN 'workspace'
           WHEN sd.schema_type_code = 'PUBLISHER' THEN UPPER(sd.code)
           WHEN sd.schema_type_code IN ('WORKSPACE', 'APPLICATION', 'FEATURE', 'MICROSERVICE')
               THEN LOWER(REPLACE(sd.code, '_', '-'))
       END,
       sd.id,
       CASE WHEN st.lifecycle_code = 'ACTIVE' THEN sd.lifecycle_code ELSE 'INACTIVE' END,
       sd.created_at, sd.updated_at
FROM schema_definitions sd
JOIN schema_types st ON st.code = sd.schema_type_code
WHERE sd.scope_code = 'PLATFORM'
  AND sd.schema_type_code <> 'DEFAULT'
  AND sd.lifecycle_code <> 'QUARANTINED'
  AND (sd.schema_type_code IN ('WORKSPACE', 'APPLICATION', 'ENVIRONMENT', 'PUBLISHER', 'FEATURE', 'MICROSERVICE')
       OR LEFT(sd.schema_type_code, 10) = 'PUBLISHER_'
       OR LEFT(sd.schema_type_code, 8) = 'FEATURE_'
       OR RIGHT(sd.schema_type_code, 5) = '_TYPE');

-- The existing, published DEFAULT schema from V17 becomes the initial database
-- fallback for each resource category. Administrators can replace each binding
-- independently without deploying code.
INSERT INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), resource_types.resource_type, 'DEFAULT', sd.id, sd.lifecycle_code,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions sd
CROSS JOIN (
    SELECT 'WORKSPACE' AS resource_type UNION ALL
    SELECT 'APPLICATION' UNION ALL
    SELECT 'ENVIRONMENT' UNION ALL
    SELECT 'PUBLISHER' UNION ALL
    SELECT 'FEATURE' UNION ALL
    SELECT 'MICROSERVICE' UNION ALL
    SELECT 'CATALOG'
) resource_types
WHERE sd.scope_code = 'PLATFORM' AND sd.schema_type_code = 'DEFAULT';

-- The old unique key included schema_type_code; preserve colliding schema rows
-- by giving only the conflicting codes a deterministic, valid suffix.
UPDATE schema_definitions sd
JOIN (
    SELECT scope_code, owner_key, code
    FROM schema_definitions
    GROUP BY scope_code, owner_key, code
    HAVING COUNT(*) > 1
) collisions ON collisions.scope_code = sd.scope_code
            AND collisions.owner_key = sd.owner_key
            AND collisions.code = sd.code
SET sd.code = CONCAT(LEFT(sd.code, 30), '-', LEFT(MD5(sd.schema_type_code), 8));

ALTER TABLE schema_definitions DROP FOREIGN KEY fk_schema_definitions_type;
-- MySQL needs an index beginning with scope_code while the scope foreign key exists.
-- Install its replacement before removing the old composite unique index.
ALTER TABLE schema_definitions
    ADD CONSTRAINT uk_schema_definitions_scope_owner_code UNIQUE (scope_code, owner_key, code);
ALTER TABLE schema_definitions DROP INDEX uk_schema_definitions_scope_owner_type_code;
ALTER TABLE schema_definitions DROP INDEX uk_schema_definitions_platform_type;
ALTER TABLE schema_definitions DROP INDEX idx_schema_definitions_type_scope_owner;
ALTER TABLE schema_definitions DROP COLUMN platform_schema_type_code;
ALTER TABLE schema_definitions DROP COLUMN schema_type_code;
DROP TABLE schema_types;


-- Current schema statements sourced from V29__schema_configuration_dynamic_resource_type.sql
-- Existing V28 installations already have the seven original resource categories.
-- Future domains can now register their own resource_type and DEFAULT binding.
ALTER TABLE schema_configuration
    DROP CHECK ck_schema_configuration_resource_type;

ALTER TABLE schema_configuration
    MODIFY COLUMN resource_type VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    MODIFY COLUMN resource_code VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;


-- Current schema statements sourced from V30__settings_native_json.sql
-- Standardize every current `settings` column on the native MySQL JSON type.
-- This keeps the database contract aligned with the API/schema contract and
-- also covers catalog tables created by earlier migrations.
-- Existing invalid JSON intentionally makes this migration fail instead of
-- silently preserving data that violates the contract.

DELIMITER $$

CREATE PROCEDURE migrate_settings_columns_to_json()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE table_name_value VARCHAR(64);
    DECLARE nullable_value VARCHAR(3);
    DECLARE settings_columns CURSOR FOR
        SELECT TABLE_NAME, IS_NULLABLE
          FROM INFORMATION_SCHEMA.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND COLUMN_NAME = 'settings'
           AND DATA_TYPE <> 'json';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN settings_columns;

    settings_loop: LOOP
        FETCH settings_columns INTO table_name_value, nullable_value;
        IF done THEN
            LEAVE settings_loop;
        END IF;

        SET @alter_settings_sql = CONCAT(
            'ALTER TABLE `', REPLACE(table_name_value, '`', '``'),
            '` MODIFY COLUMN `settings` JSON ',
            IF(nullable_value = 'YES', 'NULL', 'NOT NULL')
        );

        PREPARE alter_settings_statement FROM @alter_settings_sql;
        EXECUTE alter_settings_statement;
        DEALLOCATE PREPARE alter_settings_statement;
    END LOOP;

    CLOSE settings_columns;
END$$

DELIMITER ;

CALL migrate_settings_columns_to_json();
DROP PROCEDURE migrate_settings_columns_to_json;


-- Current schema statements sourced from V31__resource_specific_tags.sql
CREATE TABLE workspace_tag (
    id BIGINT NOT NULL AUTO_INCREMENT,
    workspace_id BIGINT NOT NULL,
    name VARCHAR(150) COLLATE utf8mb4_unicode_ci NOT NULL,
    origin_type VARCHAR(20) COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_workspace_tag UNIQUE (workspace_id, name),
    CONSTRAINT fk_workspace_tag_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
    CONSTRAINT ck_workspace_tag_origin_type CHECK (origin_type IN ('MANUAL', 'SYSTEM')),
    KEY idx_workspace_tag_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE application_tag (
    id BIGINT NOT NULL AUTO_INCREMENT,
    application_id BIGINT NOT NULL,
    name VARCHAR(150) COLLATE utf8mb4_unicode_ci NOT NULL,
    origin_type VARCHAR(20) COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_application_tag UNIQUE (application_id, name),
    CONSTRAINT fk_application_tag_application FOREIGN KEY (application_id) REFERENCES applications(id),
    CONSTRAINT ck_application_tag_origin_type CHECK (origin_type IN ('MANUAL', 'SYSTEM')),
    KEY idx_application_tag_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- Current schema statements sourced from V32__settings_for_microservice_and_publisher.sql
-- Complete the settings contract for resources that did not yet persist settings.
-- Existing rows receive an empty JSON object so the new contract remains NOT NULL.

ALTER TABLE platform_microservices ADD COLUMN settings JSON NULL AFTER description;
UPDATE platform_microservices SET settings = JSON_OBJECT() WHERE settings IS NULL;
ALTER TABLE platform_microservices MODIFY COLUMN settings JSON NOT NULL;

ALTER TABLE publishers ADD COLUMN settings JSON NULL AFTER deprecated;
UPDATE publishers SET settings = JSON_OBJECT() WHERE settings IS NULL;
ALTER TABLE publishers MODIFY COLUMN settings JSON NOT NULL;


-- Current schema statements sourced from V33__workspace_authorizer_group_visibility.sql
UPDATE workspaces
SET authorizer_group = NULLIF(UPPER(TRIM(authorizer_group)), '')
WHERE authorizer_group IS NOT NULL;

CREATE INDEX idx_workspaces_authorizer_group
    ON workspaces (authorizer_group);


-- Current schema statements sourced from V34__application_resource_schema.sql
-- Golden POC: full structural contract for Application.
INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES
    (0, UUID(), 'PLATFORM', 'application-resource', 'Application Resource', 'ACTIVE',
     CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), sd.id, 1, 'Application Resource v1',
       '{
          "type":"object",
          "additionalProperties":false,
          "required":["name","alias","acronym","applicationScope"],
          "properties":{
            "version":{"type":"integer","minimum":0},
            "name":{"type":"string","minLength":3,"maxLength":100},
            "alias":{"type":"string","minLength":1,"maxLength":100},
            "acronym":{"type":"string","minLength":1,"maxLength":20},
            "applicationScope":{"type":"string","enum":["BACKEND","FRONTEND","SHARED"]},
            "authorizerGroup":{"type":"string","minLength":1,"maxLength":255},
            "settings":{"type":"object","additionalProperties":true},
            "tags":{
              "type":"array",
              "uniqueItems":true,
              "items":{
                "type":"string",
                "minLength":1,
                "maxLength":150,
                "pattern":"^[a-z0-9]+(-[a-z0-9]+)*$"
              }
            }
          }
        }',
       'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions sd
WHERE sd.scope_code = 'PLATFORM'
  AND sd.code = 'application-resource';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'APPLICATION', 'application', sd.id, 'ACTIVE',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions sd
WHERE sd.scope_code = 'PLATFORM'
  AND sd.code = 'application-resource';


-- Current schema statements sourced from V35__platform_resource_schema_view.sql
-- Public read contract consumed by platform-schema-validation.
-- Ownership remains in workspace-service; consumers know only this VIEW.
CREATE OR REPLACE VIEW vw_platform_resource_schemas AS
SELECT sc.resource_type,
       sc.resource_code,
       sv.schema_version,
       sv.definition
FROM schema_configuration sc
JOIN schema_definitions sd
  ON sd.id = sc.schema_id
JOIN schema_versions sv
  ON sv.schema_id = sd.id
WHERE sc.lifecycle_code = 'ACTIVE'
  AND sd.lifecycle_code = 'ACTIVE'
  AND sv.status = 'PUBLISHED'
  AND sv.schema_version = (
      SELECT MAX(sv2.schema_version)
      FROM schema_versions sv2
      WHERE sv2.schema_id = sd.id
        AND sv2.status = 'PUBLISHED'
  );


-- Current schema statements sourced from V36__create_audit_outbox.sql
CREATE TABLE audit_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT,
    identifier VARCHAR(36) NOT NULL,
    payload JSON NOT NULL,
    metadata JSON NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    claimed_at DATETIME(6) NULL,
    claimed_by VARCHAR(100) NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    processed_at DATETIME(6) NULL,
    CONSTRAINT PK_AUDIT_OUTBOX PRIMARY KEY (id),
    CONSTRAINT UK_AUDIT_OUTBOX_IDENTIFIER UNIQUE (identifier),
    INDEX IDX_AUDIT_OUTBOX_STATUS_CREATED (status, created_at, id),
    INDEX IDX_AUDIT_OUTBOX_RETRY (status, next_attempt_at)
) ENGINE=InnoDB;


-- Current schema statements sourced from V37__create_publication_mode_catalog.sql
CREATE TABLE type_publication_modes (
    code VARCHAR(30) NOT NULL,
    label VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order INT NOT NULL,
    is_active BOOLEAN NOT NULL,
    settings TEXT NOT NULL,
    CONSTRAINT pk_type_publication_modes PRIMARY KEY (code),
    CONSTRAINT ck_type_publication_modes_code CHECK (code IN ('AUTOMATIC', 'MANUAL'))
);

INSERT INTO
    type_publication_modes (
        code,
        label,
        description,
        sort_order,
        is_active,
        settings
    )
VALUES
    (
        'AUTOMATIC',
        'Automática',
        'Propaga publicações para os destinos mapeados.',
        1,
        true,
        '{}'
    ),
    (
        'MANUAL',
        'Manual',
        'Permite publicação explícita pelo destino.',
        2,
        true,
        '{}'
    );

DELETE FROM type_sharing_statuses
WHERE
    code IN (
        'WAITING_DESTINATION_APPROVAL',
        'WAITING_SOURCE_APPROVAL',
        'CANCELLED',
        'NOT_REQUESTED'
    );

INSERT INTO
    type_sharing_statuses (
        code,
        label,
        description,
        sort_order,
        is_active,
        settings
    )
VALUES
    (
        'PENDING',
        'Pendente',
        'Solicitação aguardando decisão do proprietário.',
        1,
        true,
        '{}'
    ),
    (
        'APPROVED',
        'Aprovada',
        'Participação autorizada enquanto contrato e mapeamento estiverem aptos.',
        2,
        true,
        '{}'
    ),
    (
        'REJECTED',
        'Rejeitada',
        'Solicitação rejeitada pelo proprietário.',
        3,
        true,
        '{}'
    ),
    (
        'REVOKED',
        'Revogada',
        'Participação aprovada e posteriormente revogada.',
        4,
        true,
        '{}'
    );


-- Current schema statements sourced from V38__create_shared_contracts.sql
CREATE TABLE shared_contracts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL,
    owner_workspace_identifier VARCHAR(36) NOT NULL,
    owner_application_identifier VARCHAR(36) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500) NULL,
    lifecycle_code VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_shared_contracts_identifier UNIQUE (identifier),
    CONSTRAINT uk_shared_contracts_scope_name UNIQUE (
        owner_workspace_identifier,
        owner_application_identifier,
        name
    ),
    INDEX idx_shared_contracts_owner_scope (
        owner_workspace_identifier,
        owner_application_identifier,
        lifecycle_code
    )
);

CREATE TABLE shared_participations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL,
    contract_id BIGINT NOT NULL,
    participant_workspace_identifier VARCHAR(36) NOT NULL,
    participant_application_identifier VARCHAR(36) NOT NULL,
    status_code VARCHAR(30) NOT NULL,
    publication_mode_code VARCHAR(30) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_shared_participations_identifier UNIQUE (identifier),
    CONSTRAINT uk_shared_participations_contract_application UNIQUE (contract_id, participant_application_identifier),
    CONSTRAINT fk_shared_participations_contract FOREIGN KEY (contract_id) REFERENCES shared_contracts (id) ON DELETE CASCADE,
    CONSTRAINT fk_shared_participations_status FOREIGN KEY (status_code) REFERENCES type_sharing_statuses (code),
    CONSTRAINT fk_shared_participations_publication_mode FOREIGN KEY (publication_mode_code) REFERENCES type_publication_modes (code),
    INDEX idx_shared_participations_participant (
        participant_workspace_identifier,
        participant_application_identifier,
        status_code
    )
);

CREATE TABLE shared_environment_mappings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    participation_id BIGINT NOT NULL,
    source_environment_identifier VARCHAR(36) NOT NULL,
    destination_environment_identifier VARCHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_shared_mapping_destination UNIQUE (
        participation_id,
        destination_environment_identifier
    ),
    CONSTRAINT fk_shared_mapping_participation FOREIGN KEY (participation_id) REFERENCES shared_participations (id) ON DELETE CASCADE
);


-- Current schema statements sourced from V39__shared_internal_references.sql
ALTER TABLE shared_contracts
ADD COLUMN owner_workspace_id BIGINT NULL,
ADD COLUMN owner_application_id BIGINT NULL;

UPDATE shared_contracts c
JOIN workspaces w ON w.identifier = c.owner_workspace_identifier
JOIN applications a ON a.identifier = c.owner_application_identifier
AND a.workspace_id = w.id
SET
    c.owner_workspace_id = w.id,
    c.owner_application_id = a.id;

ALTER TABLE shared_contracts
DROP INDEX uk_shared_contracts_scope_name,
DROP INDEX idx_shared_contracts_owner_scope,
MODIFY owner_workspace_id BIGINT NOT NULL,
MODIFY owner_application_id BIGINT NOT NULL,
DROP COLUMN owner_workspace_identifier,
DROP COLUMN owner_application_identifier,
ADD CONSTRAINT uk_shared_contracts_scope_name UNIQUE (owner_workspace_id, owner_application_id, name),
ADD INDEX idx_shared_contracts_owner_scope (
    owner_workspace_id,
    owner_application_id,
    lifecycle_code
),
ADD CONSTRAINT fk_shared_contracts_workspace FOREIGN KEY (owner_workspace_id) REFERENCES workspaces (id),
ADD CONSTRAINT fk_shared_contracts_application FOREIGN KEY (owner_application_id) REFERENCES applications (id);

ALTER TABLE shared_participations
ADD COLUMN participant_workspace_id BIGINT NULL,
ADD COLUMN participant_application_id BIGINT NULL;

UPDATE shared_participations p
JOIN workspaces w ON w.identifier = p.participant_workspace_identifier
JOIN applications a ON a.identifier = p.participant_application_identifier
AND a.workspace_id = w.id
SET
    p.participant_workspace_id = w.id,
    p.participant_application_id = a.id;

ALTER TABLE shared_participations
DROP INDEX uk_shared_participations_contract_application,
DROP INDEX idx_shared_participations_participant,
MODIFY participant_workspace_id BIGINT NOT NULL,
MODIFY participant_application_id BIGINT NOT NULL,
DROP COLUMN participant_workspace_identifier,
DROP COLUMN participant_application_identifier,
ADD CONSTRAINT uk_shared_participations_contract_application UNIQUE (contract_id, participant_application_id),
ADD INDEX idx_shared_participations_participant (
    participant_workspace_id,
    participant_application_id,
    status_code
),
ADD CONSTRAINT fk_shared_participations_workspace FOREIGN KEY (participant_workspace_id) REFERENCES workspaces (id),
ADD CONSTRAINT fk_shared_participations_application FOREIGN KEY (participant_application_id) REFERENCES applications (id);

ALTER TABLE shared_environment_mappings
ADD COLUMN source_environment_id BIGINT NULL,
ADD COLUMN destination_environment_id BIGINT NULL;

UPDATE shared_environment_mappings m
JOIN environments source ON source.identifier = m.source_environment_identifier
JOIN environments destination ON destination.identifier = m.destination_environment_identifier
SET
    m.source_environment_id = source.id,
    m.destination_environment_id = destination.id;

ALTER TABLE shared_environment_mappings
DROP INDEX uk_shared_mapping_destination,
MODIFY source_environment_id BIGINT NOT NULL,
MODIFY destination_environment_id BIGINT NOT NULL,
DROP COLUMN source_environment_identifier,
DROP COLUMN destination_environment_identifier,
ADD CONSTRAINT uk_shared_mapping_destination UNIQUE (participation_id, destination_environment_id),
ADD CONSTRAINT fk_shared_mapping_source_environment FOREIGN KEY (source_environment_id) REFERENCES environments (id),
ADD CONSTRAINT fk_shared_mapping_destination_environment FOREIGN KEY (destination_environment_id) REFERENCES environments (id);


-- Current schema statements sourced from V40__shared_feature_reference.sql
ALTER TABLE platform_features
ADD COLUMN shareable BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE shared_contracts
ADD COLUMN feature_id BIGINT NOT NULL,
ADD INDEX idx_shared_contracts_feature (feature_id),
ADD CONSTRAINT fk_shared_contracts_feature FOREIGN KEY (feature_id) REFERENCES platform_features (id);


-- Current schema statements sourced from V41__shared_contract_feature_identity.sql
ALTER TABLE shared_contracts
DROP INDEX uk_shared_contracts_scope_name,
DROP COLUMN name,
ADD CONSTRAINT uk_shared_contracts_scope_feature UNIQUE (
    owner_workspace_id,
    owner_application_id,
    feature_id
);


-- Current schema statements sourced from V42__workspace_resource_schema.sql
-- One structural contract for Workspace creation and update.
INSERT IGNORE INTO
    schema_definitions (
        version,
        identifier,
        scope_code,
        code,
        name,
        lifecycle_code,
        created_at,
        updated_at
    )
VALUES
    (
        0,
        UUID(),
        'PLATFORM',
        'workspace-resource',
        'Workspace Resource',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );

INSERT IGNORE INTO
    schema_versions (
        identifier,
        schema_id,
        schema_version,
        version_name,
        definition,
        status,
        created_at
    )
SELECT
    UUID(),
    id,
    1,
    'Workspace Resource v1',
    '{
    "type": "object",
    "additionalProperties": false,
    "required": [
        "workspaceType",
        "name",
        "description",
        "requester",
        "acronym",
        "emailGroup",
        "approvers"
    ],
    "properties": {
        "workspaceType": {
            "type": "string",
            "minLength": 1,
            "enum": [
                "ADMIN",
                "MANAGER"
            ],
            "maxLength": 20
        },
        "name": {
            "type": "string",
            "minLength": 3,
            "maxLength": 80,
            "pattern": "\\\\S"
        },
        "description": {
            "type": "string",
            "minLength": 10,
            "maxLength": 200,
            "pattern": "\\\\S"
        },
        "requester": {
            "type": "string",
            "minLength": 5,
            "maxLength": 10,
            "pattern": "\\\\S"
        },
        "acronym": {
            "type": "string",
            "minLength": 1,
            "maxLength": 5,
            "pattern": "\\\\S"
        },
        "authorizerGroup": {
            "type": [
                "string",
                "null"
            ],
            "maxLength": 20
        },
        "settings": {
            "type": [
                "object",
                "null"
            ],
            "additionalProperties": true
        },
        "emailGroup": {
            "type": "string",
            "minLength": 1,
            "maxLength": 200,
            "pattern": "^[^\\\\s@]+@[^\\\\s@]+\\\\.[^\\\\s@]+$",
            "format": "email"
        },
        "approvers": {
            "type": "array",
            "minItems": 1,
            "items": {
                "type": "object",
                "additionalProperties": false,
                "required": [
                    "functional",
                    "email"
                ],
                "properties": {
                    "functional": {
                        "type": "string",
                        "minLength": 1,
                        "maxLength": 10,
                        "pattern": "\\\\S"
                    },
                    "email": {
                        "type": "string",
                        "minLength": 1,
                        "maxLength": 200,
                        "pattern": "^[^\\\\s@]+@[^\\\\s@]+\\\\.[^\\\\s@]+$",
                        "format": "email"
                    }
                }
            },
            "maxItems": 5
        },
        "tags": {
            "type": [
                "array",
                "null"
            ],
            "items": {
                "type": "string",
                "maxLength": 50
            },
            "maxItems": 20
        },
        "version": {
            "type": [
                "integer",
                "null"
            ],
            "minimum": 0,
            "maximum": 9223372036854775807
        }
    },
    "title": "Workspace",
    "$schema": "https://json-schema.org/draft/2020-12/schema"
}
',
    'PUBLISHED',
    CURRENT_TIMESTAMP
FROM
    schema_definitions
WHERE
    scope_code = 'PLATFORM'
    AND code = 'workspace-resource';

INSERT IGNORE INTO
    schema_configuration (
        version,
        identifier,
        resource_type,
        resource_code,
        schema_id,
        lifecycle_code,
        created_at,
        updated_at
    )
SELECT
    0,
    UUID(),
    'WORKSPACE',
    'workspace',
    id,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM
    schema_definitions
WHERE
    scope_code = 'PLATFORM'
    AND code = 'workspace-resource';


-- Current schema statements sourced from V43__workspace_column_limits.sql
-- Match Workspace storage limits to its structural resource schema.
ALTER TABLE workspaces
DROP FOREIGN KEY fk_workspaces_workspace_type,
DROP FOREIGN KEY fk_workspaces_lifecycle;

ALTER TABLE workspaces
MODIFY COLUMN workspace_type_code VARCHAR(20) NOT NULL,
MODIFY COLUMN name VARCHAR(80) NOT NULL,
MODIFY COLUMN description VARCHAR(200) NOT NULL,
MODIFY COLUMN requester VARCHAR(10) NOT NULL,
MODIFY COLUMN authorizer_group VARCHAR(20) NULL,
MODIFY COLUMN email_group VARCHAR(200) NOT NULL,
MODIFY COLUMN lifecycle_code VARCHAR(20) NOT NULL,
ADD CONSTRAINT fk_workspaces_workspace_type FOREIGN KEY (workspace_type_code) REFERENCES type_workspaces (code),
ADD CONSTRAINT fk_workspaces_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle (code);

ALTER TABLE workspace_approvers
MODIFY COLUMN functional VARCHAR(10) NOT NULL,
MODIFY COLUMN email VARCHAR(200) NOT NULL;

-- System tags include the Workspace name (up to 80 characters).
-- Manual tags are limited to 50 characters by the Workspace resource schema.
ALTER TABLE workspace_tag
MODIFY COLUMN name VARCHAR(80) COLLATE utf8mb4_unicode_ci NOT NULL;


-- Current schema statements sourced from V44__workspace_supported_types.sql
-- Workspace types are restricted to ADMIN and MANAGER.
UPDATE type_workspaces
SET
    is_active = false
WHERE
    code = 'CATALOG';

ALTER TABLE workspaces
ADD CONSTRAINT ck_workspaces_supported_type CHECK (workspace_type_code IN ('ADMIN', 'MANAGER'));


-- Current schema statements sourced from V45__workspace_name_case_insensitive.sql
-- Enforce global case-insensitive Workspace name uniqueness regardless of database defaults.
-- The existing uk_workspaces_name index uses the column collation.
ALTER TABLE workspaces
MODIFY COLUMN name VARCHAR(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;


-- Current schema statements sourced from V46__workspace_onboarding_registration.sql
CREATE TABLE workspace_onboarding_completions
(
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    workspace_id         BIGINT       NOT NULL,
    onboarding_phase_code VARCHAR(50)  NOT NULL,
    user_identifier      VARCHAR(200) NOT NULL,
    completed_at         DATETIME(6)  NOT NULL,
    CONSTRAINT pk_workspace_onboarding_completions PRIMARY KEY (id),
    CONSTRAINT uk_workspace_onboarding_phase UNIQUE (workspace_id, onboarding_phase_code),
    CONSTRAINT fk_workspace_onboarding_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces (id),
    CONSTRAINT fk_workspace_onboarding_phase FOREIGN KEY (onboarding_phase_code) REFERENCES type_onboardings (code)
);

INSERT INTO type_onboardings (code, label, description, sort_order, is_active, settings)
VALUES
    ('WORKSPACE_REGISTRATION', 'Cadastro do workspace', 'Cadastro dos dados principais do workspace.', 1, TRUE, '{"orientation":"Revise os dados principais do workspace."}'),
    ('WORKSPACE_FIRST_ENVIRONMENT', 'Configuração do ambiente do workspace', 'Configuração do primeiro ambiente do workspace.', 2, TRUE, '{"orientation":"Configure o ambiente padrão do workspace quando a configuração de ambientes estiver disponível."}'),
    ('FIRST_APPLICATION_REGISTRATION', 'Cadastro da primeira aplicação', 'Cadastro da primeira aplicação vinculada ao workspace.', 3, TRUE, '{"orientation":"Cadastre a primeira aplicação vinculada ao workspace."}'),
    ('APPLICATION_FIRST_ENVIRONMENT', 'Configuração do ambiente da aplicação', 'Configuração do primeiro ambiente da aplicação.', 4, TRUE, '{"orientation":"Configure o ambiente padrão da aplicação quando a configuração de ambientes estiver disponível."}');


-- Current schema statements sourced from V47__application_json_and_resource_schema.sql
-- Persist structured settings and align the published contract with the reference JSON Schema.
ALTER TABLE applications
    MODIFY settings JSON NOT NULL,
    MODIFY name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;

INSERT INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), sd.id, 2, 'Application Resource v2',
    '
{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Application",
    "type": "object",
    "additionalProperties": false,
    "required": [
        "name",
        "alias",
        "acronym",
        "applicationScope"
    ],
    "properties": {
        "version": {
            "type": "integer",
            "minimum": 0,
            "maximum": 9223372036854775807
        },
        "name": {
            "type": "string",
            "minLength": 3,
            "maxLength": 100,
            "pattern": "\\\\S"
        },
        "alias": {
            "type": "string",
            "minLength": 1,
            "maxLength": 100,
            "pattern": "\\\\S"
        },
        "acronym": {
            "type": "string",
            "minLength": 1,
            "maxLength": 20,
            "pattern": "\\\\S"
        },
        "applicationScope": {
            "type": "string",
            "enum": [
                "BACKEND",
                "FRONTEND",
                "SHARED"
            ]
        },
        "authorizerGroup": {
            "type": "string",
            "minLength": 1,
            "maxLength": 255
        },
        "settings": {
            "type": "object",
            "additionalProperties": true
        },
        "tags": {
            "type": "array",
            "maxItems": 20,
            "items": {
                "type": "string",
                "minLength": 1,
                "maxLength": 150
            }
        }
    }
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions sd
WHERE sd.scope_code = 'PLATFORM' AND sd.code = 'application-resource';


-- Current schema statements sourced from V48__application_column_limits.sql
-- Align equivalent Application attributes with the Workspace storage/schema standard.
ALTER TABLE applications
    DROP FOREIGN KEY fk_applications_scope,
    DROP FOREIGN KEY fk_applications_lifecycle;

ALTER TABLE applications
    MODIFY name VARCHAR(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    MODIFY alias VARCHAR(80) NOT NULL,
    MODIFY acronym VARCHAR(5) NOT NULL,
    MODIFY authorizer_group VARCHAR(20) NULL,
    MODIFY application_scope_code VARCHAR(20) NOT NULL,
    MODIFY lifecycle_code VARCHAR(20) NOT NULL,
    MODIFY settings JSON NOT NULL,
    ADD CONSTRAINT fk_applications_scope FOREIGN KEY (application_scope_code) REFERENCES type_application_scopes(code),
    ADD CONSTRAINT fk_applications_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code);

-- System tags contain names/aliases up to 80 characters; manual tags are limited to 50 by the schema.
ALTER TABLE application_tag
    MODIFY name VARCHAR(80) COLLATE utf8mb4_unicode_ci NOT NULL;

INSERT INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 3, 'Application Resource v3',
    '{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Application",
    "type": "object",
    "additionalProperties": false,
    "required": [
        "name",
        "alias",
        "acronym",
        "applicationScope",
        "settings"
    ],
    "properties": {
        "version": {
            "type": [
                "integer",
                "null"
            ],
            "minimum": 0,
            "maximum": 9223372036854775807
        },
        "name": {
            "type": "string",
            "minLength": 3,
            "maxLength": 80,
            "pattern": "\\\\S"
        },
        "alias": {
            "type": "string",
            "minLength": 1,
            "maxLength": 80,
            "pattern": "\\\\S"
        },
        "acronym": {
            "type": "string",
            "minLength": 1,
            "maxLength": 5,
            "pattern": "\\\\S"
        },
        "applicationScope": {
            "type": "string",
            "enum": [
                "BACKEND",
                "FRONTEND",
                "SHARED"
            ],
            "minLength": 1,
            "maxLength": 20
        },
        "authorizerGroup": {
            "type": [
                "string",
                "null"
            ],
            "maxLength": 20
        },
        "settings": {
            "type": "object",
            "additionalProperties": false,
            "required": [
                "language",
                "infrastructure"
            ],
            "properties": {
                "language": {
                    "type": "string",
                    "enum": [
                        "JAVA",
                        "C#",
                        "NODE",
                        "GO"
                    ]
                },
                "infrastructure": {
                    "type": "string",
                    "enum": [
                        "VM",
                        "ECS",
                        "EKS"
                    ]
                }
            }
        },
        "tags": {
            "type": [
                "array",
                "null"
            ],
            "maxItems": 20,
            "items": {
                "type": "string",
                "maxLength": 50
            }
        }
    }
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions
WHERE scope_code = 'PLATFORM' AND code = 'application-resource';


-- Current schema statements sourced from V49__workspace_application_attribute_limits.sql
-- Align Workspace and Application attribute limits; add Application description.
ALTER TABLE workspaces
    MODIFY name VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    MODIFY authorizer_group VARCHAR(10) NULL,
    MODIFY email_group VARCHAR(100) NOT NULL;
ALTER TABLE workspace_approvers MODIFY email VARCHAR(100) NOT NULL;
ALTER TABLE applications
    MODIFY name VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    MODIFY alias VARCHAR(50) NOT NULL,
    MODIFY authorizer_group VARCHAR(10) NULL,
    ADD COLUMN description VARCHAR(200) NULL AFTER alias;
ALTER TABLE workspace_tag MODIFY name VARCHAR(50) COLLATE utf8mb4_unicode_ci NOT NULL;
ALTER TABLE application_tag MODIFY name VARCHAR(50) COLLATE utf8mb4_unicode_ci NOT NULL;

INSERT INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 4, 'Application Resource v4',
    '{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Application",
    "type": "object",
    "additionalProperties": false,
    "required": [
        "name",
        "alias",
        "acronym",
        "applicationScope",
        "settings"
    ],
    "properties": {
        "version": {
            "type": [
                "integer",
                "null"
            ],
            "minimum": 0,
            "maximum": 9223372036854775807
        },
        "name": {
            "type": "string",
            "minLength": 3,
            "maxLength": 50,
            "pattern": "\\\\S"
        },
        "alias": {
            "type": "string",
            "minLength": 1,
            "maxLength": 50,
            "pattern": "\\\\S"
        },
        "acronym": {
            "type": "string",
            "minLength": 1,
            "maxLength": 5,
            "pattern": "\\\\S"
        },
        "applicationScope": {
            "type": "string",
            "enum": [
                "BACKEND",
                "FRONTEND",
                "SHARED"
            ],
            "minLength": 1,
            "maxLength": 20
        },
        "authorizerGroup": {
            "type": [
                "string",
                "null"
            ],
            "maxLength": 10
        },
        "settings": {
            "type": "object",
            "additionalProperties": false,
            "required": [
                "language",
                "infrastructure"
            ],
            "properties": {
                "language": {
                    "type": "string",
                    "enum": [
                        "JAVA",
                        "C#",
                        "NODE",
                        "GO"
                    ]
                },
                "infrastructure": {
                    "type": "string",
                    "enum": [
                        "VM",
                        "ECS",
                        "EKS"
                    ]
                }
            }
        },
        "tags": {
            "type": [
                "array",
                "null"
            ],
            "maxItems": 20,
            "items": {
                "type": "string",
                "maxLength": 50
            }
        },
        "description": {
            "type": [
                "string",
                "null"
            ],
            "maxLength": 200
        }
    }
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code='PLATFORM' AND code='application-resource';

INSERT INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 2, 'Workspace Resource v2',
    '{
    "type": "object",
    "additionalProperties": false,
    "required": [
        "workspaceType",
        "name",
        "description",
        "requester",
        "acronym",
        "emailGroup",
        "approvers"
    ],
    "properties": {
        "workspaceType": {
            "type": "string",
            "minLength": 1,
            "enum": [
                "ADMIN",
                "MANAGER"
            ],
            "maxLength": 20
        },
        "name": {
            "type": "string",
            "minLength": 3,
            "maxLength": 50,
            "pattern": "\\\\S"
        },
        "description": {
            "type": "string",
            "minLength": 10,
            "maxLength": 200,
            "pattern": "\\\\S"
        },
        "requester": {
            "type": "string",
            "minLength": 5,
            "maxLength": 10,
            "pattern": "\\\\S"
        },
        "acronym": {
            "type": "string",
            "minLength": 1,
            "maxLength": 5,
            "pattern": "\\\\S"
        },
        "authorizerGroup": {
            "type": [
                "string",
                "null"
            ],
            "maxLength": 10
        },
        "settings": {
            "type": [
                "object",
                "null"
            ],
            "additionalProperties": true
        },
        "emailGroup": {
            "type": "string",
            "minLength": 1,
            "maxLength": 100,
            "pattern": "^[^\\\\s@]+@[^\\\\s@]+\\\\.[^\\\\s@]+$",
            "format": "email"
        },
        "approvers": {
            "type": "array",
            "minItems": 1,
            "items": {
                "type": "object",
                "additionalProperties": false,
                "required": [
                    "functional",
                    "email"
                ],
                "properties": {
                    "functional": {
                        "type": "string",
                        "minLength": 1,
                        "maxLength": 10,
                        "pattern": "\\\\S"
                    },
                    "email": {
                        "type": "string",
                        "minLength": 1,
                        "maxLength": 100,
                        "pattern": "^[^\\\\s@]+@[^\\\\s@]+\\\\.[^\\\\s@]+$",
                        "format": "email"
                    }
                }
            },
            "maxItems": 5
        },
        "tags": {
            "type": [
                "array",
                "null"
            ],
            "items": {
                "type": "string",
                "maxLength": 50
            },
            "maxItems": 20
        },
        "version": {
            "type": [
                "integer",
                "null"
            ],
            "minimum": 0,
            "maximum": 9223372036854775807
        }
    },
    "title": "Workspace",
    "$schema": "https://json-schema.org/draft/2020-12/schema"
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code='PLATFORM' AND code='workspace-resource';


-- Current schema statements sourced from V50__publisher_resource_schema_and_limits.sql
-- One request schema for Publisher creation and update, including conditional settings.
ALTER TABLE publishers MODIFY description VARCHAR(200) NOT NULL;
INSERT IGNORE INTO schema_definitions (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES (0, UUID(), 'PLATFORM', 'publisher-resource', 'Publisher Resource', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT IGNORE INTO schema_versions (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 1, 'Publisher Resource v1', '{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Publisher request",
    "type": "object",
    "additionalProperties": false,
    "required": [
        "name",
        "description",
        "scope",
        "settings",
        "code"
    ],
    "properties": {
        "code": {
            "type": "string",
            "pattern": "^[A-Z][A-Z0-9_]{0,39}$",
            "maxLength": 40
        },
        "name": {
            "type": "string",
            "minLength": 3,
            "maxLength": 50,
            "pattern": "\\\\S"
        },
        "description": {
            "type": "string",
            "minLength": 1,
            "maxLength": 200,
            "pattern": "\\\\S"
        },
        "scope": {
            "type": "string",
            "enum": [
                "WORKSPACE",
                "APPLICATION"
            ],
            "maxLength": 50
        },
        "deprecated": {
            "type": "boolean"
        },
        "settings": {
            "type": "object"
        },
        "version": {
            "type": [
                "integer",
                "null"
            ],
            "minimum": 0,
            "maximum": 9223372036854775807
        }
    },
    "allOf": [
        {
            "if": {
                "properties": {
                    "code": {
                        "const": "KAFKA"
                    }
                },
                "required": [
                    "code"
                ]
            },
            "then": {
                "properties": {
                    "settings": {
                        "type": "object",
                        "required": [
                            "topic",
                            "address"
                        ],
                        "properties": {
                            "topic": {
                                "type": "string",
                                "minLength": 1,
                                "maxLength": 249,
                                "pattern": "\\\\S"
                            },
                            "address": {
                                "type": "string",
                                "minLength": 1,
                                "maxLength": 1000,
                                "pattern": "\\\\S"
                            }
                        },
                        "additionalProperties": false
                    }
                }
            }
        },
        {
            "if": {
                "properties": {
                    "code": {
                        "const": "APPCONFIG"
                    }
                },
                "required": [
                    "code"
                ]
            },
            "then": {
                "properties": {
                    "settings": {
                        "type": "object",
                        "required": [
                            "arn",
                            "name"
                        ],
                        "properties": {
                            "arn": {
                                "type": "string",
                                "minLength": 1,
                                "maxLength": 2048,
                                "pattern": "\\\\S"
                            },
                            "name": {
                                "type": "string",
                                "minLength": 1,
                                "maxLength": 64,
                                "pattern": "\\\\S"
                            }
                        },
                        "additionalProperties": false
                    }
                }
            }
        },
        {
            "if": {
                "properties": {
                    "code": {
                        "const": "HTTP"
                    }
                },
                "required": [
                    "code"
                ]
            },
            "then": {
                "properties": {
                    "settings": {
                        "type": "object",
                        "required": [
                            "clientId",
                            "timeout"
                        ],
                        "properties": {
                            "clientId": {
                                "type": "string",
                                "minLength": 1,
                                "maxLength": 100,
                                "pattern": "\\\\S"
                            },
                            "timeout": {
                                "type": "integer",
                                "minimum": 1
                            }
                        },
                        "additionalProperties": false
                    }
                }
            }
        }
    ]
}', 'PUBLISHED', CURRENT_TIMESTAMP FROM schema_definitions WHERE scope_code='PLATFORM' AND code='publisher-resource';
INSERT IGNORE INTO schema_configuration (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'PUBLISHER', 'publisher', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM schema_definitions WHERE scope_code='PLATFORM' AND code='publisher-resource';


-- Current schema statements sourced from V51__publisher_code_column_limit.sql
ALTER TABLE publishers
    MODIFY COLUMN code VARCHAR(50) NOT NULL,
    DROP CHECK ck_publishers_code;

INSERT INTO schema_versions (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT
    UUID(),
    schema_id,
    2,
    'Publisher Resource v2',
    JSON_SET(
        definition,
        '$.properties.code',
        JSON_OBJECT('type', 'string', 'enum', JSON_ARRAY('KAFKA', 'HTTP', 'APPCONFIG'), 'maxLength', 50),
        '$.allOf[2].then.properties.settings.properties.timeout.maximum',
        60000,
        '$.allOf[2].then.properties.settings.properties.timeout.description',
        'Timeout in milliseconds.'
    ),
    'PUBLISHED',
    CURRENT_TIMESTAMP
FROM schema_versions
WHERE schema_version = 1
  AND status = 'PUBLISHED'
  AND schema_id = (
      SELECT id FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'publisher-resource'
  );


-- Current schema statements sourced from V52__environment_resource_schemas.sql
-- Publish one complete request schema for each environment resource.
INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES
    (0, UUID(), 'PLATFORM', 'environment-resource', 'Environment Resource', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (0, UUID(), 'PLATFORM', 'environment-type-resource', 'Environment Type Resource', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (0, UUID(), 'PLATFORM', 'environment-type-compatibility-resource', 'Environment Type Compatibility Resource', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), sd.id, 1, 'Environment Resource v1',
    '{
      "$schema":"https://json-schema.org/draft/2020-12/schema",
      "title":"Environment request",
      "type":"object",
      "additionalProperties":false,
      "required":["name","description","authorizationType","settings"],
      "properties":{
        "version":{"type":["integer","null"],"minimum":0,"maximum":9223372036854775807},
        "name":{"type":"string","minLength":3,"maxLength":50,"pattern":"\\\\S"},
        "description":{"type":"string","minLength":3,"maxLength":250,"pattern":"\\\\S"},
        "authorizationType":{"type":"string","enum":["DEV","TST","ADM"],"maxLength":50},
        "sortOrder":{"type":["integer","null"],"minimum":1,"maximum":2147483647},
        "authorizerGroup":{"type":["string","null"],"maxLength":253},
        "settings":{"type":"object","description":"Environment-specific settings; properties are configurable by the platform.","additionalProperties":true},
        "environmentTypeCode":{"type":["string","null"],"minLength":1,"maxLength":50,"pattern":"\\\\S"},
        "parentIdentifier":{"type":["string","null"],"minLength":1,"maxLength":36,"pattern":"\\\\S"}
      }
    }',
    'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions sd
WHERE sd.scope_code = 'PLATFORM' AND sd.code = 'environment-resource';

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), sd.id, 1, 'Environment Type Resource v1',
    '{
      "$schema":"https://json-schema.org/draft/2020-12/schema",
      "title":"Environment type request",
      "type":"object",
      "additionalProperties":false,
      "required":["code","name","description","rootAllowed","allowsChildren","workspaceRequired","displayOrder"],
      "properties":{
        "version":{"type":["integer","null"],"minimum":0,"maximum":9223372036854775807},
        "code":{"type":"string","enum":["DEFAULT","CUSTOM","SHARD","CELL"],"maxLength":50},
        "name":{"type":"string","minLength":1,"maxLength":100,"pattern":"\\\\S"},
        "description":{"type":"string","minLength":1,"maxLength":250,"pattern":"\\\\S"},
        "rootAllowed":{"type":"boolean"},
        "allowsChildren":{"type":"boolean"},
        "workspaceRequired":{"type":"boolean"},
        "displayOrder":{"type":"integer","minimum":0,"maximum":2147483647}
      }
    }',
    'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions sd
WHERE sd.scope_code = 'PLATFORM' AND sd.code = 'environment-type-resource';

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), sd.id, 1, 'Environment Type Compatibility Resource v1',
    '{
      "$schema":"https://json-schema.org/draft/2020-12/schema",
      "title":"Environment type compatibility request",
      "type":"object",
      "additionalProperties":false,
      "required":["parentTypeCode","childTypeCode"],
      "properties":{
        "parentTypeCode":{"type":"string","minLength":1,"maxLength":50,"pattern":"\\\\S"},
        "childTypeCode":{"type":"string","minLength":1,"maxLength":50,"pattern":"\\\\S"}
      }
    }',
    'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions sd
WHERE sd.scope_code = 'PLATFORM' AND sd.code = 'environment-type-compatibility-resource';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'ENVIRONMENT', bindings.resource_code, sd.id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions sd
CROSS JOIN (
    SELECT 'environment-resource' AS definition_code, 'environment' AS resource_code UNION ALL
    SELECT 'environment-type-resource', 'environment-type' UNION ALL
    SELECT 'environment-type-compatibility-resource', 'environment-type-compatibility'
) bindings
WHERE sd.scope_code = 'PLATFORM' AND sd.code = bindings.definition_code;


-- Current schema statements sourced from V53__environment_json_settings_and_name_limit.sql
-- Environment names are limited to 50 characters by the published request contract.
-- Settings are JSON documents and are now persisted as native JSON values.
ALTER TABLE environments
    MODIFY name VARCHAR(50) NOT NULL,
    MODIFY settings JSON NOT NULL;


-- Current schema statements sourced from V54__environment_type_allows_children.sql
ALTER TABLE environment_types
    ADD COLUMN allows_children BOOLEAN NOT NULL DEFAULT FALSE AFTER root_allowed;

UPDATE environment_types
SET allows_children = EXISTS (
    SELECT 1
    FROM environment_type_compatibilities compatibility
    WHERE compatibility.parent_type_id = environment_types.id
      AND compatibility.lifecycle_code = 'ACTIVE'
) OR EXISTS (
    SELECT 1
    FROM environments parent_environment
    JOIN environments child_environment
      ON child_environment.parent_environment_id = parent_environment.id
    WHERE parent_environment.environment_type_id = environment_types.id
);

INSERT INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), sd.id, 2, 'Environment Type Resource v2',
    '{
      "$schema":"https://json-schema.org/draft/2020-12/schema",
      "title":"Environment type request",
      "type":"object",
      "additionalProperties":false,
      "required":["code","name","description","rootAllowed","allowsChildren","workspaceRequired","displayOrder"],
      "properties":{
        "version":{"type":["integer","null"],"minimum":0,"maximum":9223372036854775807},
        "code":{"type":"string","enum":["DEFAULT","CUSTOM","SHARD","CELL"],"maxLength":50},
        "name":{"type":"string","minLength":1,"maxLength":100,"pattern":"\\\\S"},
        "description":{"type":"string","minLength":1,"maxLength":250,"pattern":"\\\\S"},
        "rootAllowed":{"type":"boolean"},
        "allowsChildren":{"type":"boolean"},
        "workspaceRequired":{"type":"boolean"},
        "displayOrder":{"type":"integer","minimum":0,"maximum":2147483647}
      }
    }',
    'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions sd
WHERE sd.scope_code = 'PLATFORM' AND sd.code = 'environment-type-resource';


-- Current schema statements sourced from V55__environment_type_compatibility_identifiers.sql
INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), sd.id, 2, 'Environment Type Compatibility Resource v2',
    '{
      "$schema":"https://json-schema.org/draft/2020-12/schema",
      "title":"Environment type compatibility request",
      "type":"object",
      "additionalProperties":false,
      "required":["parentTypeIdentifier","childTypeIdentifier"],
      "properties":{
        "parentTypeIdentifier":{"type":"string","format":"uuid"},
        "childTypeIdentifier":{"type":"string","format":"uuid"}
      }
    }',
    'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions sd
WHERE sd.scope_code = 'PLATFORM' AND sd.code = 'environment-type-compatibility-resource';


-- Current schema statements sourced from V56__environment_type_code_primary_key.sql
-- Preserve existing references while replacing the surrogate type ID with the immutable business code.
ALTER TABLE environments DROP FOREIGN KEY fk_environments_type;
ALTER TABLE environment_type_compatibilities
    DROP FOREIGN KEY fk_environment_compatibility_parent,
    DROP FOREIGN KEY fk_environment_compatibility_child;

ALTER TABLE environments ADD COLUMN environment_type_code VARCHAR(50) NULL;
UPDATE environments environment
JOIN environment_types type ON type.id = environment.environment_type_id
SET environment.environment_type_code = type.code;

ALTER TABLE environment_type_compatibilities
    ADD COLUMN parent_type_code VARCHAR(50) NULL,
    ADD COLUMN child_type_code VARCHAR(50) NULL;
UPDATE environment_type_compatibilities compatibility
JOIN environment_types parent_type ON parent_type.id = compatibility.parent_type_id
JOIN environment_types child_type ON child_type.id = compatibility.child_type_id
SET compatibility.parent_type_code = parent_type.code,
    compatibility.child_type_code = child_type.code;

ALTER TABLE environment_type_compatibilities DROP INDEX uk_environment_compatibility;
ALTER TABLE environment_type_compatibilities
    DROP COLUMN parent_type_id,
    DROP COLUMN child_type_id,
    MODIFY parent_type_code VARCHAR(50) NOT NULL,
    MODIFY child_type_code VARCHAR(50) NOT NULL,
    ADD CONSTRAINT uk_environment_compatibility UNIQUE (parent_type_code, child_type_code);

ALTER TABLE environments
    DROP COLUMN environment_type_id,
    MODIFY environment_type_code VARCHAR(50) NOT NULL;

-- MySQL requires removing AUTO_INCREMENT before dropping the old primary key.
ALTER TABLE environment_types MODIFY id BIGINT NOT NULL;
ALTER TABLE environment_types
    DROP PRIMARY KEY,
    DROP INDEX identifier,
    DROP INDEX code,
    DROP COLUMN id,
    DROP COLUMN identifier,
    ADD PRIMARY KEY (code);

ALTER TABLE environment_type_compatibilities
    ADD CONSTRAINT fk_environment_compatibility_parent FOREIGN KEY (parent_type_code) REFERENCES environment_types(code),
    ADD CONSTRAINT fk_environment_compatibility_child FOREIGN KEY (child_type_code) REFERENCES environment_types(code);
ALTER TABLE environments
    ADD CONSTRAINT fk_environments_type FOREIGN KEY (environment_type_code) REFERENCES environment_types(code);

ALTER TABLE environment_types
    ADD CONSTRAINT chk_environment_type_builtin_scope CHECK (
        (code <> 'DEFAULT' OR (root_allowed AND NOT workspace_required))
        AND (code <> 'CUSTOM' OR (root_allowed AND workspace_required))
        AND (code NOT IN ('SHARD', 'CELL') OR (NOT root_allowed AND workspace_required))
    );


-- Current schema statements sourced from V57__environment_type_code_schemas.sql
INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), sd.id, 2, 'Environment Resource v2',
    '{
      "$schema":"https://json-schema.org/draft/2020-12/schema",
      "title":"Environment request",
      "type":"object",
      "additionalProperties":false,
      "required":["name","description","authorizationType","settings","environmentTypeCode"],
      "properties":{
        "version":{"type":["integer","null"],"minimum":0,"maximum":9223372036854775807},
        "name":{"type":"string","minLength":3,"maxLength":50,"pattern":"\\\\S"},
        "description":{"type":"string","minLength":3,"maxLength":200,"pattern":"\\\\S"},
        "authorizationType":{"type":"string","enum":["DEV","TST","ADM"],"maxLength":50},
        "sortOrder":{"type":["integer","null"],"minimum":1,"maximum":2147483647},
        "authorizerGroup":{"type":["string","null"],"maxLength":253},
        "settings":{"type":"object","description":"Environment-specific settings; properties are configurable by the platform.","additionalProperties":true},
        "environmentTypeCode":{"type":"string","enum":["DEFAULT","CUSTOM","SHARD","CELL"],"maxLength":50},
        "parentIdentifier":{"type":["string","null"],"minLength":1,"maxLength":36,"format":"uuid"}
      }
    }',
    'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions sd
WHERE sd.scope_code = 'PLATFORM' AND sd.code = 'environment-resource';

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), sd.id, 3, 'Environment Type Resource v3',
    '{
      "$schema":"https://json-schema.org/draft/2020-12/schema",
      "title":"Environment type request",
      "type":"object",
      "additionalProperties":false,
      "required":["code","name","description","rootAllowed","allowsChildren","workspaceRequired","displayOrder"],
      "properties":{
        "version":{"type":["integer","null"],"minimum":0,"maximum":9223372036854775807},
        "code":{"type":"string","enum":["DEFAULT","CUSTOM","SHARD","CELL"],"minLength":1,"maxLength":50,"pattern":"\\\\S"},
        "name":{"type":"string","minLength":1,"maxLength":100,"pattern":"\\\\S"},
        "description":{"type":"string","minLength":1,"maxLength":250,"pattern":"\\\\S"},
        "rootAllowed":{"type":"boolean"},
        "allowsChildren":{"type":"boolean"},
        "workspaceRequired":{"type":"boolean"},
        "displayOrder":{"type":"integer","minimum":0,"maximum":2147483647}
      }
    }',
    'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions sd
WHERE sd.scope_code = 'PLATFORM' AND sd.code = 'environment-type-resource';

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), sd.id, 3, 'Environment Type Compatibility Resource v3',
    '{"$schema":"https://json-schema.org/draft/2020-12/schema","title":"Environment type compatibility request","type":"object","additionalProperties":false,"required":["parentTypeCode","childTypeCode"],"properties":{"parentTypeCode":{"type":"string","enum":["DEFAULT","CUSTOM","SHARD","CELL"],"minLength":1,"maxLength":50,"pattern":"\\\\S"},"childTypeCode":{"type":"string","enum":["DEFAULT","CUSTOM","SHARD","CELL"],"minLength":1,"maxLength":50,"pattern":"\\\\S"}}}',
    'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions sd
WHERE sd.scope_code = 'PLATFORM' AND sd.code = 'environment-type-compatibility-resource';


-- Current schema statements sourced from V58__message_resource_schemas.sql
-- Publish shared create/update request contracts for message and translation.

INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES (0, UUID(), 'PLATFORM', 'message-resource', 'Message request', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 1, 'message v1',
    '{"$schema": "https://json-schema.org/draft/2020-12/schema", "title": "Message request", "type": "object", "additionalProperties": false, "required": ["microserviceIdentifier", "messageKey", "code", "httpStatus"], "properties": {"version": {"type": ["integer", "null"], "minimum": 0, "maximum": 9223372036854775807}, "microserviceIdentifier": {"type": "string", "format": "uuid", "minLength": 36, "maxLength": 36}, "messageKey": {"type": "string", "minLength": 1, "maxLength": 255, "pattern": "^[a-z0-9][a-z0-9._-]{0,254}$"}, "code": {"type": "string", "minLength": 3, "maxLength": 50, "pattern": "^[A-Z][A-Z0-9-]{2,49}$"}, "httpStatus": {"type": "integer", "minimum": 100, "maximum": 599}, "observation": {"type": ["string", "null"], "maxLength": 500}, "translations": {"type": ["array", "null"], "maxItems": 100, "items": {"type": "object", "additionalProperties": false, "required": ["locale", "title", "detail", "suggestion"], "properties": {"version": {"type": ["integer", "null"], "minimum": 0, "maximum": 9223372036854775807}, "locale": {"type": "string", "minLength": 2, "maxLength": 35, "pattern": "^[A-Za-z0-9]+(-[A-Za-z0-9]+)*$"}, "title": {"type": "string", "minLength": 1, "maxLength": 150, "pattern": "\\\\S"}, "detail": {"type": "string", "minLength": 1, "maxLength": 1000, "pattern": "\\\\S"}, "suggestion": {"type": "string", "minLength": 1, "maxLength": 1000, "pattern": "\\\\S"}}}}}, "allOf": [{"if": {"required": ["version"], "properties": {"version": {"type": "integer"}}}, "then": {"not": {"required": ["translations"]}}}]}',
    'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'message-resource';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'MESSAGE', 'message', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'message-resource';

INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES (0, UUID(), 'PLATFORM', 'message-translation-resource', 'Message translation request', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 1, 'message-translation v1',
    '{"$schema": "https://json-schema.org/draft/2020-12/schema", "title": "Message translation request", "type": "object", "additionalProperties": false, "required": ["locale", "title", "detail", "suggestion"], "properties": {"version": {"type": ["integer", "null"], "minimum": 0, "maximum": 9223372036854775807}, "locale": {"type": "string", "minLength": 2, "maxLength": 35, "pattern": "^[A-Za-z0-9]+(-[A-Za-z0-9]+)*$"}, "title": {"type": "string", "minLength": 1, "maxLength": 150, "pattern": "\\\\S"}, "detail": {"type": "string", "minLength": 1, "maxLength": 1000, "pattern": "\\\\S"}, "suggestion": {"type": "string", "minLength": 1, "maxLength": 1000, "pattern": "\\\\S"}}}',
    'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'message-translation-resource';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'MESSAGE', 'message-translation', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'message-translation-resource';


-- Current schema statements sourced from V59__message_physical_deletion_lifecycle.sql
-- Preserve legacy quarantined records as inactive; future DELETE removes them physically.
UPDATE messages
SET lifecycle_code = 'INACTIVE', version = version + 1, updated_at = CURRENT_TIMESTAMP(6)
WHERE lifecycle_code = 'QUARANTINED';

UPDATE message_translations
SET lifecycle_code = 'INACTIVE', version = version + 1, updated_at = CURRENT_TIMESTAMP(6)
WHERE lifecycle_code = 'QUARANTINED';

ALTER TABLE messages
    ADD CONSTRAINT ck_messages_supported_lifecycle
        CHECK (lifecycle_code IN ('ACTIVE', 'INACTIVE'));

ALTER TABLE message_translations
    ADD CONSTRAINT ck_message_translations_supported_lifecycle
        CHECK (lifecycle_code IN ('ACTIVE', 'INACTIVE'));


-- Current schema statements sourced from V60__onboarding_requester.sql
-- Validate existing history before reducing the column size; do not truncate identities.
ALTER TABLE workspace_onboarding_completions
    ADD CONSTRAINT chk_onboarding_requester
        CHECK (CHAR_LENGTH(TRIM(user_identifier)) > 0 AND CHAR_LENGTH(user_identifier) <= 10);

ALTER TABLE workspace_onboarding_completions
    DROP CHECK chk_onboarding_requester,
    CHANGE COLUMN user_identifier requester VARCHAR(10) NOT NULL,
    ADD CONSTRAINT chk_onboarding_requester
        CHECK (CHAR_LENGTH(TRIM(requester)) > 0 AND CHAR_LENGTH(requester) <= 10);


-- Current schema statements sourced from V61__onboarding_phase_settings_schema.sql
-- Publish the settings contract consumed by onboarding and catalog administration.
INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES (0, UUID(), 'PLATFORM', 'onboarding-phase-type-settings', 'Onboarding phase settings',
        'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 1, 'Onboarding phase settings v1',
    '{"$schema":"https://json-schema.org/draft/2020-12/schema","title":"Onboarding phase settings","type":"object","additionalProperties":true,"properties":{"orientation":{"type":["string","null"]}}}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'onboarding-phase-type-settings';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'CATALOG', 'onboarding-phase-type', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'onboarding-phase-type-settings';


-- Current schema statements sourced from V62__microservice_resource_schema.sql
-- Separate HTTP resource contracts from per-microservice settings schemas.
INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES (0, UUID(), 'PLATFORM', 'microservice-resource', 'Microservice Resource',
        'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 1, 'Microservice Resource v1',
    '{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Microservice request",
    "type": "object",
    "additionalProperties": false,
    "required": [
        "code",
        "name",
        "settings"
    ],
    "properties": {
        "version": {
            "type": [
                "integer",
                "null"
            ],
            "minimum": 0,
            "maximum": 9223372036854775807
        },
        "code": {
            "type": "string",
            "maxLength": 50,
            "pattern": "^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$"
        },
        "name": {
            "type": "string",
            "minLength": 1,
            "maxLength": 100,
            "pattern": "\\\\S"
        },
        "description": {
            "type": [
                "string",
                "null"
            ],
            "maxLength": 500
        },
        "settings": {
            "type": "object"
        }
    }
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'microservice-resource';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'PLATFORM', 'microservice', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'microservice-resource';


-- Current schema statements sourced from V63__feature_resource_schema.sql
-- Separate the feature HTTP contract from per-feature settings schemas.
INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES (0, UUID(), 'PLATFORM', 'feature-resource', 'Feature Resource',
        'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 1, 'Feature Resource v1',
    '{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Feature request",
    "type": "object",
    "additionalProperties": false,
    "required": [
        "code",
        "name",
        "settings",
        "microserviceIdentifier"
    ],
    "properties": {
        "version": {
            "type": [
                "integer",
                "null"
            ],
            "minimum": 0,
            "maximum": 9223372036854775807
        },
        "code": {
            "type": "string",
            "maxLength": 50,
            "pattern": "^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$"
        },
        "name": {
            "type": "string",
            "minLength": 1,
            "maxLength": 100,
            "pattern": "\\\\S"
        },
        "description": {
            "type": [
                "string",
                "null"
            ],
            "maxLength": 500
        },
        "settings": {
            "type": "object"
        },
        "microserviceIdentifier": {
            "type": "string",
            "format": "uuid"
        },
        "shareable": {
            "type": "boolean",
            "default": false
        }
    }
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'feature-resource';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'PLATFORM', 'feature', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'feature-resource';


-- Current schema statements sourced from V64__context_resource_schema.sql
-- Publish the context HTTP contract without changing existing context data.
INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES (0, UUID(), 'PLATFORM', 'context-resource', 'Context Resource',
        'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 1, 'Context Resource v1',
    '{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Context request",
    "type": "object",
    "additionalProperties": false,
    "required": [
        "code",
        "name"
    ],
    "properties": {
        "version": {
            "type": [
                "integer",
                "null"
            ],
            "minimum": 0,
            "maximum": 9223372036854775807
        },
        "code": {
            "type": "string",
            "maxLength": 50,
            "pattern": "^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$"
        },
        "name": {
            "type": "string",
            "minLength": 1,
            "maxLength": 100,
            "pattern": "\\\\S"
        },
        "description": {
            "type": [
                "string",
                "null"
            ],
            "maxLength": 500
        },
        "featureIdentifiers": {
            "type": [
                "array",
                "null"
            ],
            "maxItems": 100,
            "items": {
                "type": "string",
                "format": "uuid"
            }
        }
    }
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'context-resource';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'PLATFORM', 'context', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'context-resource';


-- Current schema statements sourced from V65__separate_feature_context_resource_schema.sql
-- Separate context attributes from feature/context association management.
-- Existing contexts, relations and applied schema versions remain unchanged.
INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 2, 'Context Resource v2', '{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Context request",
    "type": "object",
    "additionalProperties": false,
    "required": [
        "code",
        "name"
    ],
    "properties": {
        "version": {
            "type": [
                "integer",
                "null"
            ],
            "minimum": 0,
            "maximum": 9223372036854775807
        },
        "code": {
            "type": "string",
            "maxLength": 50,
            "pattern": "^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$"
        },
        "name": {
            "type": "string",
            "minLength": 1,
            "maxLength": 100,
            "pattern": "\\\\S"
        },
        "description": {
            "type": [
                "string",
                "null"
            ],
            "maxLength": 500
        }
    }
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'context-resource';

INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES (0, UUID(), 'PLATFORM', 'feature-context-resource', 'Feature Context Resource',
        'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 1, 'Feature Context Resource v1', '{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Feature context associations request",
    "type": "object",
    "additionalProperties": false,
    "required": [
        "version",
        "featureIdentifiers"
    ],
    "properties": {
        "version": {
            "type": "integer",
            "minimum": 0,
            "maximum": 9223372036854775807
        },
        "featureIdentifiers": {
            "type": "array",
            "maxItems": 100,
            "items": {
                "type": "string",
                "format": "uuid"
            }
        }
    }
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'feature-context-resource';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'PLATFORM', 'feature-context', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'feature-context-resource';


-- Current schema statements sourced from V66__create_segments_and_categories.sql
-- Global segment catalog. No workspace/application association and no deletion cascades.
CREATE TABLE segment
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    lifecycle_code VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_segment PRIMARY KEY (id),
    CONSTRAINT uk_segment_identifier UNIQUE (identifier),
    CONSTRAINT uk_segment_code UNIQUE (code),
    CONSTRAINT ck_segment_code CHECK (REGEXP_LIKE(code, '^[a-z0-9]+(-[a-z0-9]+)*$', 'c')),
    CONSTRAINT ck_segment_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    CONSTRAINT ck_segment_lifecycle CHECK (lifecycle_code IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT fk_segment_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code)
);

CREATE TABLE segment_category
(
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    segment_id BIGINT NOT NULL,
    lifecycle_code VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_segment_category PRIMARY KEY (id),
    CONSTRAINT uk_segment_category_identifier UNIQUE (identifier),
    CONSTRAINT uk_segment_category_code UNIQUE (segment_id, code),
    CONSTRAINT ck_segment_category_code CHECK (REGEXP_LIKE(code, '^[a-z0-9]+(-[a-z0-9]+)*$', 'c')),
    CONSTRAINT ck_segment_category_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    CONSTRAINT ck_segment_category_lifecycle CHECK (lifecycle_code IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT fk_segment_category_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code),
    CONSTRAINT fk_segment_category_segment FOREIGN KEY (segment_id) REFERENCES segment(id)
);


-- Current schema statements sourced from V67__segment_resource_schemas.sql
-- Publish the global segment/category contracts using existing PLATFORM schema infrastructure.
INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES (0, UUID(), 'PLATFORM', 'segment-resource', 'Segment Resource', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 1, 'Segment Resource v1', '{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Segment request",
    "type": "object",
    "additionalProperties": false,
    "required": [
        "code",
        "name"
    ],
    "properties": {
        "version": {
            "type": [
                "integer",
                "null"
            ],
            "minimum": 0,
            "maximum": 9223372036854775807
        },
        "code": {
            "type": "string",
            "maxLength": 100,
            "pattern": "^[a-z0-9]+(?:-[a-z0-9]+)*$"
        },
        "name": {
            "type": "string",
            "minLength": 1,
            "maxLength": 150,
            "pattern": "\\\\S"
        }
    }
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'segment-resource';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'PLATFORM', 'segment', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'segment-resource';

INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES (0, UUID(), 'PLATFORM', 'segment-category-resource', 'Segment Category Resource', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 1, 'Segment Category Resource v1', '{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Segment Category request",
    "type": "object",
    "additionalProperties": false,
    "required": [
        "code",
        "name"
    ],
    "properties": {
        "version": {
            "type": [
                "integer",
                "null"
            ],
            "minimum": 0,
            "maximum": 9223372036854775807
        },
        "code": {
            "type": "string",
            "maxLength": 100,
            "pattern": "^[a-z0-9]+(?:-[a-z0-9]+)*$"
        },
        "name": {
            "type": "string",
            "minLength": 1,
            "maxLength": 150,
            "pattern": "\\\\S"
        }
    }
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'segment-category-resource';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'PLATFORM', 'segment-category', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'segment-category-resource';


-- Current schema statements sourced from V68__configuration_catalogs_and_platform_microservice.sql
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


-- Current schema statements sourced from V69__create_configurations.sql
CREATE TABLE configurations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL,
    microservice_id BIGINT NOT NULL,
    configuration_key VARCHAR(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    description VARCHAR(200),
    type_code VARCHAR(50) NOT NULL,
    value JSON NOT NULL,
    published_value JSON,
    published_type_code VARCHAR(50),
    status_code VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_configurations PRIMARY KEY (id),
    CONSTRAINT uk_configuration_identifier UNIQUE (identifier),
    CONSTRAINT uk_configuration_microservice_key UNIQUE (microservice_id, configuration_key),
    CONSTRAINT ck_configuration_key CHECK (REGEXP_LIKE(configuration_key, '^[A-Za-z0-9]+([._-][A-Za-z0-9]+)*$', 'c')),
    CONSTRAINT ck_configuration_snapshot CHECK (
        (published_value IS NULL AND published_type_code IS NULL)
        OR (published_value IS NOT NULL AND published_type_code IS NOT NULL)
    ),
    CONSTRAINT ck_configuration_inactive CHECK (
        active = true OR (status_code = 'DRAFT' AND published_value IS NULL AND published_type_code IS NULL)
    ),
    CONSTRAINT ck_configuration_published CHECK (
        status_code <> 'PUBLISHED' OR (active = true AND published_value IS NOT NULL)
    ),
    CONSTRAINT fk_configuration_microservice FOREIGN KEY (microservice_id) REFERENCES platform_microservices(id),
    CONSTRAINT fk_configuration_type FOREIGN KEY (type_code) REFERENCES type_configuration_types(code),
    CONSTRAINT fk_configuration_published_type FOREIGN KEY (published_type_code) REFERENCES type_configuration_types(code),
    CONSTRAINT fk_configuration_status FOREIGN KEY (status_code) REFERENCES type_configuration_statuses(code)
);

CREATE INDEX idx_configuration_active_id ON configurations (active, id);


-- Current schema statements sourced from V70__configuration_resource_schemas.sql
INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES (0, UUID(), 'PLATFORM', 'configuration-resource', 'configuration-resource', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 1, 'configuration-resource v1', '{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Configuration request",
    "type": "object",
    "additionalProperties": false,
    "required": [
        "type",
        "value"
    ],
    "properties": {
        "version": {
            "type": [
                "integer",
                "null"
            ],
            "minimum": 0,
            "maximum": 9223372036854775807
        },
        "microserviceIdentifier": {
            "type": "string",
            "format": "uuid",
            "maxLength": 36
        },
        "key": {
            "type": "string",
            "maxLength": 80,
            "pattern": "^[A-Za-z0-9]+(?:[._-][A-Za-z0-9]+)*$"
        },
        "description": {
            "type": [
                "string",
                "null"
            ],
            "maxLength": 200
        },
        "type": {
            "type": "string",
            "enum": [
                "CONFIGURATION",
                "EXPRESSION"
            ]
        },
        "value": {}
    },
    "oneOf": [
        {
            "required": [
                "microserviceIdentifier",
                "key"
            ],
            "properties": {
                "version": {
                    "type": "null"
                }
            }
        },
        {
            "required": [
                "version"
            ],
            "properties": {
                "version": {
                    "type": "integer"
                },
                "microserviceIdentifier": false,
                "key": false
            }
        }
    ]
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'configuration-resource';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'PLATFORM', 'configuration', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'configuration-resource';

INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES (0, UUID(), 'PLATFORM', 'configuration-expression', 'configuration-expression', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 1, 'configuration-expression v1', '{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Configuration Expression V1",
    "type": "object",
    "required": [
        "operator",
        "valueType",
        "value"
    ],
    "properties": {
        "operator": {
            "enum": [
                "EQUALS",
                "NOT_EQUALS",
                "CONTAINS",
                "NOT_CONTAINS",
                "GREATER_THAN",
                "GREATER_THAN_OR_EQUALS",
                "LESS_THAN",
                "LESS_THAN_OR_EQUALS"
            ]
        },
        "valueType": {
            "enum": [
                "STRING",
                "INTEGER",
                "LONG",
                "DECIMAL"
            ]
        },
        "value": {}
    },
    "allOf": [
        {
            "if": {
                "properties": {
                    "operator": {
                        "enum": [
                            "CONTAINS",
                            "NOT_CONTAINS"
                        ]
                    }
                },
                "required": [
                    "operator"
                ]
            },
            "then": {
                "properties": {
                    "value": {
                        "type": "array",
                        "minItems": 1,
                        "uniqueItems": true
                    }
                }
            },
            "else": {
                "properties": {
                    "value": {
                        "not": {
                            "type": "array"
                        }
                    }
                }
            }
        },
        {
            "if": {
                "properties": {
                    "operator": {
                        "enum": [
                            "GREATER_THAN",
                            "GREATER_THAN_OR_EQUALS",
                            "LESS_THAN",
                            "LESS_THAN_OR_EQUALS"
                        ]
                    }
                },
                "required": [
                    "operator"
                ]
            },
            "then": {
                "properties": {
                    "valueType": {
                        "enum": [
                            "INTEGER",
                            "LONG",
                            "DECIMAL"
                        ]
                    }
                }
            }
        },
        {
            "if": {
                "properties": {
                    "valueType": {
                        "const": "STRING"
                    }
                },
                "required": [
                    "valueType"
                ]
            },
            "then": {
                "properties": {
                    "value": {
                        "oneOf": [
                            {
                                "type": "string"
                            },
                            {
                                "type": "array",
                                "items": {
                                    "type": "string"
                                }
                            }
                        ]
                    }
                }
            }
        },
        {
            "if": {
                "properties": {
                    "valueType": {
                        "const": "INTEGER"
                    }
                },
                "required": [
                    "valueType"
                ]
            },
            "then": {
                "properties": {
                    "value": {
                        "oneOf": [
                            {
                                "type": "integer",
                                "minimum": -2147483648,
                                "maximum": 2147483647
                            },
                            {
                                "type": "array",
                                "items": {
                                    "type": "integer",
                                    "minimum": -2147483648,
                                    "maximum": 2147483647
                                }
                            }
                        ]
                    }
                }
            }
        },
        {
            "if": {
                "properties": {
                    "valueType": {
                        "const": "LONG"
                    }
                },
                "required": [
                    "valueType"
                ]
            },
            "then": {
                "properties": {
                    "value": {
                        "oneOf": [
                            {
                                "type": "integer",
                                "minimum": -9223372036854775808,
                                "maximum": 9223372036854775807
                            },
                            {
                                "type": "array",
                                "items": {
                                    "type": "integer",
                                    "minimum": -9223372036854775808,
                                    "maximum": 9223372036854775807
                                }
                            }
                        ]
                    }
                }
            }
        },
        {
            "if": {
                "properties": {
                    "valueType": {
                        "const": "DECIMAL"
                    }
                },
                "required": [
                    "valueType"
                ]
            },
            "then": {
                "properties": {
                    "value": {
                        "oneOf": [
                            {
                                "type": "number"
                            },
                            {
                                "type": "array",
                                "items": {
                                    "type": "number"
                                }
                            }
                        ]
                    }
                }
            }
        }
    ],
    "additionalProperties": false
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'configuration-expression';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'PLATFORM', 'configuration-expression', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'configuration-expression';

INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES (0, UUID(), 'PLATFORM', 'configuration-type-settings', 'configuration-type-settings', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 1, 'configuration-type-settings v1', '{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "configuration-type settings",
    "type": "object",
    "additionalProperties": false
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'configuration-type-settings';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'CATALOG', 'configuration-type', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'configuration-type-settings';

INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES (0, UUID(), 'PLATFORM', 'configuration-status-type-settings', 'configuration-status-type-settings', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 1, 'configuration-status-type-settings v1', '{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "configuration-status-type settings",
    "type": "object",
    "additionalProperties": false
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'configuration-status-type-settings';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'CATALOG', 'configuration-status-type', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'configuration-status-type-settings';


-- Current schema statements sourced from V71__unify_configuration_resource_schema.sql
-- Ensure the catalog state required to retire the standalone contract on upgrades.
INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings)
VALUES ('INACTIVE', 'Inactive', 'Inactive lifecycle state', 2, true, '{}');

-- Publish the unified request contract without changing previously applied migrations.
INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 2, 'configuration-resource v2', '{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "Configuration request",
  "type": "object",
  "additionalProperties": false,
  "required": [
    "type",
    "value"
  ],
  "properties": {
    "version": {
      "type": [
        "integer",
        "null"
      ],
      "minimum": 0,
      "maximum": 9223372036854775807
    },
    "microserviceIdentifier": {
      "type": "string",
      "format": "uuid",
      "maxLength": 36
    },
    "key": {
      "type": "string",
      "maxLength": 80,
      "pattern": "^[A-Za-z0-9]+(?:[._-][A-Za-z0-9]+)*$"
    },
    "description": {
      "type": [
        "string",
        "null"
      ],
      "maxLength": 200
    },
    "type": {
      "type": "string",
      "enum": [
        "CONFIGURATION",
        "EXPRESSION"
      ]
    },
    "value": {}
  },
  "allOf": [
    {
      "oneOf": [
        {
          "required": [
            "microserviceIdentifier",
            "key"
          ],
          "properties": {
            "version": {
              "type": "null"
            }
          }
        },
        {
          "required": [
            "version"
          ],
          "properties": {
            "version": {
              "type": "integer"
            },
            "microserviceIdentifier": false,
            "key": false
          }
        }
      ]
    },
    {
      "if": {
        "properties": {
          "type": {
            "const": "EXPRESSION"
          }
        },
        "required": [
          "type"
        ]
      },
      "then": {
        "properties": {
          "value": {
            "type": "object",
            "additionalProperties": false,
            "required": [
              "operator",
              "valueType",
              "value"
            ],
            "properties": {
              "operator": {
                "enum": [
                  "EQUALS",
                  "NOT_EQUALS",
                  "CONTAINS",
                  "NOT_CONTAINS",
                  "GREATER_THAN",
                  "GREATER_THAN_OR_EQUALS",
                  "LESS_THAN",
                  "LESS_THAN_OR_EQUALS"
                ]
              },
              "valueType": {
                "enum": [
                  "STRING",
                  "INTEGER",
                  "LONG",
                  "DECIMAL"
                ]
              },
              "value": {}
            },
            "allOf": [
              {
                "if": {
                  "properties": {
                    "operator": {
                      "enum": [
                        "CONTAINS",
                        "NOT_CONTAINS"
                      ]
                    }
                  },
                  "required": [
                    "operator"
                  ]
                },
                "then": {
                  "properties": {
                    "value": {
                      "type": "array",
                      "minItems": 1,
                      "uniqueItems": true
                    }
                  }
                },
                "else": {
                  "properties": {
                    "value": {
                      "not": {
                        "type": "array"
                      }
                    }
                  }
                }
              },
              {
                "if": {
                  "properties": {
                    "operator": {
                      "enum": [
                        "GREATER_THAN",
                        "GREATER_THAN_OR_EQUALS",
                        "LESS_THAN",
                        "LESS_THAN_OR_EQUALS"
                      ]
                    }
                  },
                  "required": [
                    "operator"
                  ]
                },
                "then": {
                  "properties": {
                    "valueType": {
                      "enum": [
                        "INTEGER",
                        "LONG",
                        "DECIMAL"
                      ]
                    }
                  }
                }
              },
              {
                "if": {
                  "properties": {
                    "valueType": {
                      "const": "STRING"
                    }
                  },
                  "required": [
                    "valueType"
                  ]
                },
                "then": {
                  "properties": {
                    "value": {
                      "oneOf": [
                        {
                          "type": "string"
                        },
                        {
                          "type": "array",
                          "items": {
                            "type": "string"
                          }
                        }
                      ]
                    }
                  }
                }
              },
              {
                "if": {
                  "properties": {
                    "valueType": {
                      "const": "INTEGER"
                    }
                  },
                  "required": [
                    "valueType"
                  ]
                },
                "then": {
                  "properties": {
                    "value": {
                      "oneOf": [
                        {
                          "type": "integer",
                          "minimum": -2147483648,
                          "maximum": 2147483647
                        },
                        {
                          "type": "array",
                          "items": {
                            "type": "integer",
                            "minimum": -2147483648,
                            "maximum": 2147483647
                          }
                        }
                      ]
                    }
                  }
                }
              },
              {
                "if": {
                  "properties": {
                    "valueType": {
                      "const": "LONG"
                    }
                  },
                  "required": [
                    "valueType"
                  ]
                },
                "then": {
                  "properties": {
                    "value": {
                      "oneOf": [
                        {
                          "type": "integer",
                          "minimum": -9223372036854775808,
                          "maximum": 9223372036854775807
                        },
                        {
                          "type": "array",
                          "items": {
                            "type": "integer",
                            "minimum": -9223372036854775808,
                            "maximum": 9223372036854775807
                          }
                        }
                      ]
                    }
                  }
                }
              },
              {
                "if": {
                  "properties": {
                    "valueType": {
                      "const": "DECIMAL"
                    }
                  },
                  "required": [
                    "valueType"
                  ]
                },
                "then": {
                  "properties": {
                    "value": {
                      "oneOf": [
                        {
                          "type": "number"
                        },
                        {
                          "type": "array",
                          "items": {
                            "type": "number"
                          }
                        }
                      ]
                    }
                  }
                }
              }
            ]
          }
        }
      }
    }
  ]
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'configuration-resource';

-- Retire the standalone expression contract while retaining its published history.
UPDATE schema_configuration
SET lifecycle_code = 'INACTIVE', version = version + 1, updated_at = CURRENT_TIMESTAMP
WHERE resource_type = 'PLATFORM' AND resource_code = 'configuration-expression' AND lifecycle_code = 'ACTIVE';

UPDATE schema_definitions
SET lifecycle_code = 'INACTIVE', version = version + 1, updated_at = CURRENT_TIMESTAMP
WHERE scope_code = 'PLATFORM' AND code = 'configuration-expression' AND lifecycle_code = 'ACTIVE';


-- Current schema statements sourced from V72__configuration_decimal_multiple_of_schema.sql
-- Publish the approved hundredths rule in the unified Configuration request contract.
-- Keep previous published versions and existing configuration values intact.
INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 3, 'configuration-resource v3', '{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "Configuration request",
  "type": "object",
  "additionalProperties": false,
  "required": [
    "type",
    "value"
  ],
  "properties": {
    "version": {
      "type": [
        "integer",
        "null"
      ],
      "minimum": 0,
      "maximum": 9223372036854775807
    },
    "microserviceIdentifier": {
      "type": "string",
      "format": "uuid",
      "maxLength": 36
    },
    "key": {
      "type": "string",
      "maxLength": 80,
      "pattern": "^[A-Za-z0-9]+(?:[._-][A-Za-z0-9]+)*$"
    },
    "description": {
      "type": [
        "string",
        "null"
      ],
      "maxLength": 200
    },
    "type": {
      "type": "string",
      "enum": [
        "CONFIGURATION",
        "EXPRESSION"
      ]
    },
    "value": {}
  },
  "allOf": [
    {
      "oneOf": [
        {
          "required": [
            "microserviceIdentifier",
            "key"
          ],
          "properties": {
            "version": {
              "type": "null"
            }
          }
        },
        {
          "required": [
            "version"
          ],
          "properties": {
            "version": {
              "type": "integer"
            },
            "microserviceIdentifier": false,
            "key": false
          }
        }
      ]
    },
    {
      "if": {
        "properties": {
          "type": {
            "const": "EXPRESSION"
          }
        },
        "required": [
          "type"
        ]
      },
      "then": {
        "properties": {
          "value": {
            "type": "object",
            "additionalProperties": false,
            "required": [
              "operator",
              "valueType",
              "value"
            ],
            "properties": {
              "operator": {
                "enum": [
                  "EQUALS",
                  "NOT_EQUALS",
                  "CONTAINS",
                  "NOT_CONTAINS",
                  "GREATER_THAN",
                  "GREATER_THAN_OR_EQUALS",
                  "LESS_THAN",
                  "LESS_THAN_OR_EQUALS"
                ]
              },
              "valueType": {
                "enum": [
                  "STRING",
                  "INTEGER",
                  "LONG",
                  "DECIMAL"
                ]
              },
              "value": {}
            },
            "allOf": [
              {
                "if": {
                  "properties": {
                    "operator": {
                      "enum": [
                        "CONTAINS",
                        "NOT_CONTAINS"
                      ]
                    }
                  },
                  "required": [
                    "operator"
                  ]
                },
                "then": {
                  "properties": {
                    "value": {
                      "type": "array",
                      "minItems": 1,
                      "uniqueItems": true
                    }
                  }
                },
                "else": {
                  "properties": {
                    "value": {
                      "not": {
                        "type": "array"
                      }
                    }
                  }
                }
              },
              {
                "if": {
                  "properties": {
                    "operator": {
                      "enum": [
                        "GREATER_THAN",
                        "GREATER_THAN_OR_EQUALS",
                        "LESS_THAN",
                        "LESS_THAN_OR_EQUALS"
                      ]
                    }
                  },
                  "required": [
                    "operator"
                  ]
                },
                "then": {
                  "properties": {
                    "valueType": {
                      "enum": [
                        "INTEGER",
                        "LONG",
                        "DECIMAL"
                      ]
                    }
                  }
                }
              },
              {
                "if": {
                  "properties": {
                    "valueType": {
                      "const": "STRING"
                    }
                  },
                  "required": [
                    "valueType"
                  ]
                },
                "then": {
                  "properties": {
                    "value": {
                      "oneOf": [
                        {
                          "type": "string"
                        },
                        {
                          "type": "array",
                          "items": {
                            "type": "string"
                          }
                        }
                      ]
                    }
                  }
                }
              },
              {
                "if": {
                  "properties": {
                    "valueType": {
                      "const": "INTEGER"
                    }
                  },
                  "required": [
                    "valueType"
                  ]
                },
                "then": {
                  "properties": {
                    "value": {
                      "oneOf": [
                        {
                          "type": "integer",
                          "minimum": -2147483648,
                          "maximum": 2147483647
                        },
                        {
                          "type": "array",
                          "items": {
                            "type": "integer",
                            "minimum": -2147483648,
                            "maximum": 2147483647
                          }
                        }
                      ]
                    }
                  }
                }
              },
              {
                "if": {
                  "properties": {
                    "valueType": {
                      "const": "LONG"
                    }
                  },
                  "required": [
                    "valueType"
                  ]
                },
                "then": {
                  "properties": {
                    "value": {
                      "oneOf": [
                        {
                          "type": "integer",
                          "minimum": -9223372036854775808,
                          "maximum": 9223372036854775807
                        },
                        {
                          "type": "array",
                          "items": {
                            "type": "integer",
                            "minimum": -9223372036854775808,
                            "maximum": 9223372036854775807
                          }
                        }
                      ]
                    }
                  }
                }
              },
              {
                "if": {
                  "properties": {
                    "valueType": {
                      "const": "DECIMAL"
                    }
                  },
                  "required": [
                    "valueType"
                  ]
                },
                "then": {
                  "properties": {
                    "value": {
                      "oneOf": [
                        {
                          "type": "number",
                          "multipleOf": 0.01
                        },
                        {
                          "type": "array",
                          "items": {
                            "type": "number",
                            "multipleOf": 0.01
                          }
                        }
                      ]
                    }
                  }
                }
              }
            ]
          }
        }
      }
    }
  ]
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'configuration-resource';


-- Current schema statements sourced from V73__configuration_lowercase_key_schema.sql
-- Enforce lowercase keys without renaming immutable existing keys.
ALTER TABLE configurations
    DROP CHECK ck_configuration_key,
    ADD CONSTRAINT ck_configuration_key
        CHECK (REGEXP_LIKE(configuration_key, '^[a-z0-9]+([._-][a-z0-9]+)*$', 'c'));

-- Publish the reviewed request schema, retaining previous published versions.
INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 4, 'configuration-resource v4', '{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "Configuration request",
  "type": "object",
  "additionalProperties": false,
  "required": [
    "type",
    "value"
  ],
  "properties": {
    "version": {
      "type": [
        "integer",
        "null"
      ],
      "minimum": 0,
      "maximum": 9223372036854775807
    },
    "microserviceIdentifier": {
      "type": "string",
      "format": "uuid",
      "maxLength": 36
    },
    "key": {
      "type": "string",
      "maxLength": 80,
      "pattern": "^[a-z0-9]+(?:[._-][a-z0-9]+)*$"
    },
    "description": {
      "type": [
        "string",
        "null"
      ],
      "maxLength": 200
    },
    "type": {
      "type": "string",
      "enum": [
        "CONFIGURATION",
        "EXPRESSION"
      ]
    },
    "value": {}
  },
  "allOf": [
    {
      "oneOf": [
        {
          "required": [
            "microserviceIdentifier",
            "key"
          ],
          "properties": {
            "version": {
              "type": "null"
            }
          }
        },
        {
          "required": [
            "version"
          ],
          "properties": {
            "version": {
              "type": "integer"
            },
            "microserviceIdentifier": false,
            "key": false
          }
        }
      ]
    },
    {
      "if": {
        "properties": {
          "type": {
            "const": "EXPRESSION"
          }
        },
        "required": [
          "type"
        ]
      },
      "then": {
        "properties": {
          "value": {
            "type": "object",
            "additionalProperties": false,
            "required": [
              "operator",
              "valueType",
              "value"
            ],
            "properties": {
              "operator": {
                "enum": [
                  "EQUALS",
                  "NOT_EQUALS",
                  "CONTAINS",
                  "NOT_CONTAINS",
                  "GREATER_THAN",
                  "GREATER_THAN_OR_EQUALS",
                  "LESS_THAN",
                  "LESS_THAN_OR_EQUALS"
                ]
              },
              "valueType": {
                "enum": [
                  "STRING",
                  "INTEGER",
                  "LONG",
                  "DECIMAL"
                ]
              },
              "value": {}
            },
            "allOf": [
              {
                "if": {
                  "properties": {
                    "operator": {
                      "enum": [
                        "CONTAINS",
                        "NOT_CONTAINS"
                      ]
                    }
                  },
                  "required": [
                    "operator"
                  ]
                },
                "then": {
                  "properties": {
                    "value": {
                      "type": "array",
                      "minItems": 1,
                      "uniqueItems": true
                    }
                  }
                },
                "else": {
                  "properties": {
                    "value": {
                      "not": {
                        "type": "array"
                      }
                    }
                  }
                }
              },
              {
                "if": {
                  "properties": {
                    "operator": {
                      "enum": [
                        "GREATER_THAN",
                        "GREATER_THAN_OR_EQUALS",
                        "LESS_THAN",
                        "LESS_THAN_OR_EQUALS"
                      ]
                    }
                  },
                  "required": [
                    "operator"
                  ]
                },
                "then": {
                  "properties": {
                    "valueType": {
                      "enum": [
                        "INTEGER",
                        "LONG",
                        "DECIMAL"
                      ]
                    }
                  }
                }
              },
              {
                "if": {
                  "properties": {
                    "valueType": {
                      "const": "STRING"
                    }
                  },
                  "required": [
                    "valueType"
                  ]
                },
                "then": {
                  "if": {
                    "properties": {
                      "operator": {
                        "enum": [
                          "CONTAINS",
                          "NOT_CONTAINS"
                        ]
                      }
                    },
                    "required": [
                      "operator"
                    ]
                  },
                  "then": {
                    "properties": {
                      "value": {
                        "items": {
                          "type": "string"
                        }
                      }
                    }
                  },
                  "else": {
                    "properties": {
                      "value": {
                        "type": "string"
                      }
                    }
                  }
                }
              },
              {
                "if": {
                  "properties": {
                    "valueType": {
                      "const": "INTEGER"
                    }
                  },
                  "required": [
                    "valueType"
                  ]
                },
                "then": {
                  "if": {
                    "properties": {
                      "operator": {
                        "enum": [
                          "CONTAINS",
                          "NOT_CONTAINS"
                        ]
                      }
                    },
                    "required": [
                      "operator"
                    ]
                  },
                  "then": {
                    "properties": {
                      "value": {
                        "items": {
                          "type": "integer",
                          "minimum": -2147483648,
                          "maximum": 2147483647
                        }
                      }
                    }
                  },
                  "else": {
                    "properties": {
                      "value": {
                        "type": "integer",
                        "minimum": -2147483648,
                        "maximum": 2147483647
                      }
                    }
                  }
                }
              },
              {
                "if": {
                  "properties": {
                    "valueType": {
                      "const": "LONG"
                    }
                  },
                  "required": [
                    "valueType"
                  ]
                },
                "then": {
                  "if": {
                    "properties": {
                      "operator": {
                        "enum": [
                          "CONTAINS",
                          "NOT_CONTAINS"
                        ]
                      }
                    },
                    "required": [
                      "operator"
                    ]
                  },
                  "then": {
                    "properties": {
                      "value": {
                        "items": {
                          "type": "integer",
                          "minimum": -9223372036854775808,
                          "maximum": 9223372036854775807
                        }
                      }
                    }
                  },
                  "else": {
                    "properties": {
                      "value": {
                        "type": "integer",
                        "minimum": -9223372036854775808,
                        "maximum": 9223372036854775807
                      }
                    }
                  }
                }
              },
              {
                "if": {
                  "properties": {
                    "valueType": {
                      "const": "DECIMAL"
                    }
                  },
                  "required": [
                    "valueType"
                  ]
                },
                "then": {
                  "if": {
                    "properties": {
                      "operator": {
                        "enum": [
                          "CONTAINS",
                          "NOT_CONTAINS"
                        ]
                      }
                    },
                    "required": [
                      "operator"
                    ]
                  },
                  "then": {
                    "properties": {
                      "value": {
                        "items": {
                          "type": "number",
                          "multipleOf": 0.01
                        }
                      }
                    }
                  },
                  "else": {
                    "properties": {
                      "value": {
                        "type": "number",
                        "multipleOf": 0.01
                      }
                    }
                  }
                }
              }
            ]
          }
        }
      }
    }
  ]
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'configuration-resource';


-- Current schema statements sourced from V74__configuration_publication_schema.sql
INSERT IGNORE INTO schema_definitions
    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
VALUES (0, UUID(), 'PLATFORM', 'configuration-publication', 'configuration-publication', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT IGNORE INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), id, 1, 'configuration-publication v1', '{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "Configuration publication batch",
  "type": "object",
  "additionalProperties": false,
  "required": ["identifiers"],
  "properties": {
    "identifiers": {
      "type": "array",
      "minItems": 1,
      "uniqueItems": true,
      "items": {"type": "string", "format": "uuid", "maxLength": 36}
    }
  }
}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'configuration-publication';

INSERT IGNORE INTO schema_configuration
    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
SELECT 0, UUID(), 'PLATFORM', 'configuration-publication', id, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'configuration-publication';


-- Current schema statements sourced from V75__rename_configuration_to_parameter_key.sql
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


-- Current schema statements sourced from V76__dynamic_parameter_resource_names.sql
-- Rename schema identities/bindings without recreating them or modifying published definitions.
UPDATE schema_definitions
SET name = CASE code
        WHEN 'configuration-resource' THEN 'dynamic-parameter-resource'
        WHEN 'configuration-expression' THEN 'dynamic-parameter-expression'
        WHEN 'configuration-publication' THEN 'dynamic-parameter-publication'
        WHEN 'configuration-type-settings' THEN 'parameter-key-type-settings'
        WHEN 'configuration-status-type-settings' THEN 'parameter-key-status-type-settings'
    END,
    code = CASE code
        WHEN 'configuration-resource' THEN 'dynamic-parameter-resource'
        WHEN 'configuration-expression' THEN 'dynamic-parameter-expression'
        WHEN 'configuration-publication' THEN 'dynamic-parameter-publication'
        WHEN 'configuration-type-settings' THEN 'parameter-key-type-settings'
        WHEN 'configuration-status-type-settings' THEN 'parameter-key-status-type-settings'
    END
WHERE scope_code = 'PLATFORM' AND code IN ('configuration-resource', 'configuration-expression',
    'configuration-publication', 'configuration-type-settings', 'configuration-status-type-settings');

UPDATE schema_configuration
SET resource_code = CASE resource_code
        WHEN 'configuration' THEN 'dynamic-parameter'
        WHEN 'configuration-expression' THEN 'dynamic-parameter-expression'
        WHEN 'configuration-publication' THEN 'dynamic-parameter-publication'
        WHEN 'configuration-type' THEN 'parameter-key-type'
        WHEN 'configuration-status-type' THEN 'parameter-key-status-type'
    END
WHERE (resource_type = 'PLATFORM' AND resource_code IN ('configuration', 'configuration-expression', 'configuration-publication'))
    OR (resource_type = 'CATALOG' AND resource_code IN ('configuration-type', 'configuration-status-type'));

-- Publish display metadata under the new names, retaining all prior schema versions unchanged.
INSERT INTO schema_versions
    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), published.schema_id, published.schema_version + 1,
       CONCAT(definition.code, ' v', published.schema_version + 1),
       JSON_SET(published.definition, '$.title', CASE definition.code
           WHEN 'dynamic-parameter-resource' THEN 'Parameter key request'
           WHEN 'dynamic-parameter-publication' THEN 'Parameter key publication'
           WHEN 'parameter-key-type-settings' THEN 'Parameter key type settings'
           WHEN 'parameter-key-status-type-settings' THEN 'Parameter key status type settings'
       END), 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_versions published
JOIN schema_definitions definition ON definition.id = published.schema_id
JOIN (
    SELECT schema_id, MAX(schema_version) AS schema_version
    FROM schema_versions WHERE status = 'PUBLISHED' GROUP BY schema_id
) latest ON latest.schema_id = published.schema_id AND latest.schema_version = published.schema_version
WHERE definition.scope_code = 'PLATFORM'
  AND definition.code IN ('dynamic-parameter-resource', 'dynamic-parameter-publication',
                          'parameter-key-type-settings', 'parameter-key-status-type-settings')
  AND published.status = 'PUBLISHED';


-- Current schema statements sourced from V77__dynamic_bundle_names.sql
-- Rename the translation aggregate without recreating rows or changing its ownership.
DROP VIEW vw_platform_messages;
RENAME TABLE message_translations TO message_translators;
ALTER TABLE message_translators
    RENAME INDEX uk_message_translations_identifier TO uk_message_translators_identifier,
    RENAME INDEX uk_message_translations_message_locale TO uk_message_translators_message_locale,
    RENAME INDEX idx_message_translations_lifecycle TO idx_message_translators_lifecycle,
    DROP FOREIGN KEY fk_message_translations_message,
    DROP FOREIGN KEY fk_message_translations_lifecycle,
    DROP CHECK ck_message_translations_supported_lifecycle,
    ADD CONSTRAINT fk_message_translators_message FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_message_translators_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code),
    ADD CONSTRAINT ck_message_translators_supported_lifecycle CHECK (lifecycle_code IN ('ACTIVE','INACTIVE'));
CREATE VIEW vw_dynamic_bundle_messages AS
SELECT CONCAT(s.code, '.', m.message_key) AS message_key,
       m.code AS code, m.http_status AS http_status, mt.locale AS locale,
       mt.title AS title, mt.detail AS message, mt.suggestion AS solution
FROM messages m
JOIN platform_microservices s ON s.id = m.microservice_id
JOIN message_translators mt ON mt.message_id = m.id
WHERE s.lifecycle_code = 'ACTIVE' AND m.lifecycle_code = 'ACTIVE' AND mt.lifecycle_code = 'ACTIVE';
UPDATE schema_definitions SET code='message-translator-resource', name='Message translator request'
WHERE scope_code='PLATFORM' AND code='message-translation-resource';
UPDATE schema_configuration SET resource_code='message-translator'
WHERE resource_type='MESSAGE' AND resource_code='message-translation';

INSERT INTO schema_versions(identifier,schema_id,schema_version,version_name,definition,status,created_at)
SELECT UUID(), sd.id, v.latest+1, 'message canonical', '{"$schema": "https://json-schema.org/draft/2020-12/schema", "title": "Message request", "type": "object", "additionalProperties": false, "required": ["microserviceIdentifier", "messageKey", "code", "httpStatus"], "properties": {"version": {"type": ["integer", "null"], "minimum": 0, "maximum": 9223372036854775807}, "microserviceIdentifier": {"type": "string", "format": "uuid", "minLength": 36, "maxLength": 36}, "messageKey": {"type": "string", "minLength": 1, "maxLength": 255, "pattern": "^[a-z0-9][a-z0-9._-]{0,254}$"}, "code": {"type": "string", "minLength": 3, "maxLength": 50, "pattern": "^[A-Z][A-Z0-9-]{2,49}$"}, "httpStatus": {"type": "integer", "minimum": 100, "maximum": 599}, "observation": {"type": ["string", "null"], "maxLength": 500}, "translators": {"type": ["array", "null"], "maxItems": 100, "items": {"type": "object", "additionalProperties": false, "required": ["locale", "title", "detail", "suggestion"], "properties": {"version": {"type": ["integer", "null"], "minimum": 0, "maximum": 9223372036854775807}, "locale": {"type": "string", "minLength": 2, "maxLength": 35, "pattern": "^[A-Za-z0-9]+(-[A-Za-z0-9]+)*$"}, "title": {"type": "string", "minLength": 1, "maxLength": 150, "pattern": "\\\\S"}, "detail": {"type": "string", "minLength": 1, "maxLength": 1000, "pattern": "\\\\S"}, "suggestion": {"type": "string", "minLength": 1, "maxLength": 1000, "pattern": "\\\\S"}}}}}, "allOf": [{"if": {"required": ["version"], "properties": {"version": {"type": "integer"}}}, "then": {"not": {"required": ["translators"]}}}]}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions sd JOIN (SELECT schema_id,MAX(schema_version) latest FROM schema_versions GROUP BY schema_id) v ON v.schema_id=sd.id
WHERE sd.scope_code='PLATFORM' AND sd.code='message-resource';

INSERT INTO schema_versions(identifier,schema_id,schema_version,version_name,definition,status,created_at)
SELECT UUID(), sd.id, v.latest+1, 'message-translator canonical', '{"$schema": "https://json-schema.org/draft/2020-12/schema", "title": "Message translator request", "type": "object", "additionalProperties": false, "required": ["locale", "title", "detail", "suggestion"], "properties": {"version": {"type": ["integer", "null"], "minimum": 0, "maximum": 9223372036854775807}, "locale": {"type": "string", "minLength": 2, "maxLength": 35, "pattern": "^[A-Za-z0-9]+(-[A-Za-z0-9]+)*$"}, "title": {"type": "string", "minLength": 1, "maxLength": 150, "pattern": "\\\\S"}, "detail": {"type": "string", "minLength": 1, "maxLength": 1000, "pattern": "\\\\S"}, "suggestion": {"type": "string", "minLength": 1, "maxLength": 1000, "pattern": "\\\\S"}}}', 'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions sd JOIN (SELECT schema_id,MAX(schema_version) latest FROM schema_versions GROUP BY schema_id) v ON v.schema_id=sd.id
WHERE sd.scope_code='PLATFORM' AND sd.code='message-translator-resource';


-- Current schema statements sourced from V78__service_context_names.sql
-- Keep every identifier, version and association; MySQL updates incoming FK targets on rename.
DROP VIEW vw_dynamic_bundle_messages;
DROP VIEW vw_feature_runtime_config;
RENAME TABLE platform_microservices TO service_context_microservices,
             platform_features TO service_context_features,
             platform_feature_contexts TO service_context_groups,
             platform_feature_context_relations TO service_context_feature_groups;
ALTER TABLE service_context_feature_groups RENAME COLUMN feature_context_id TO group_id;
ALTER TABLE service_context_microservices RENAME INDEX fk_platform_services_lifecycle TO fk_service_context_microservices_lifecycle;
ALTER TABLE service_context_features RENAME INDEX fk_platform_features_microservice TO fk_service_context_features_microservice;
ALTER TABLE service_context_features RENAME INDEX fk_platform_features_lifecycle TO fk_service_context_features_lifecycle;
ALTER TABLE service_context_groups RENAME INDEX fk_platform_feature_contexts_lifecycle TO fk_service_context_groups_lifecycle;
ALTER TABLE service_context_feature_groups RENAME INDEX fk_platform_feature_context_relations_context TO fk_service_context_feature_groups_group;
ALTER TABLE service_context_microservices
 RENAME INDEX uk_platform_microservices_identifier TO uk_service_context_microservices_identifier,
 RENAME INDEX uk_platform_microservices_code TO uk_service_context_microservices_code,
 RENAME INDEX uk_platform_microservices_name TO uk_service_context_microservices_name;
ALTER TABLE service_context_microservices DROP FOREIGN KEY fk_platform_services_lifecycle, DROP CHECK ck_platform_microservices_code, ADD CONSTRAINT fk_service_context_microservices_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code), ADD CONSTRAINT ck_service_context_microservices_code CHECK (code REGEXP '^[a-z][a-z0-9]*(-[a-z0-9]+)*$');
ALTER TABLE service_context_features
 RENAME INDEX uk_platform_features_identifier TO uk_service_context_features_identifier,
 RENAME INDEX uk_platform_features_code TO uk_service_context_features_code,
 RENAME INDEX uk_platform_features_name TO uk_service_context_features_name;
ALTER TABLE service_context_features DROP FOREIGN KEY fk_platform_features_lifecycle, DROP CHECK ck_platform_features_code, ADD CONSTRAINT fk_service_context_features_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code), ADD CONSTRAINT ck_service_context_features_code CHECK (code REGEXP '^[a-z][a-z0-9]*(-[a-z0-9]+)*$');
ALTER TABLE service_context_groups
 RENAME INDEX uk_platform_feature_contexts_identifier TO uk_service_context_groups_identifier,
 RENAME INDEX uk_platform_feature_contexts_code TO uk_service_context_groups_code,
 RENAME INDEX uk_platform_feature_contexts_name TO uk_service_context_groups_name;
ALTER TABLE service_context_groups DROP FOREIGN KEY fk_platform_feature_contexts_lifecycle, DROP CHECK ck_platform_feature_contexts_code, ADD CONSTRAINT fk_service_context_groups_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code), ADD CONSTRAINT ck_service_context_groups_code CHECK (code REGEXP '^[a-z][a-z0-9]*(-[a-z0-9]+)*$');
ALTER TABLE service_context_features DROP FOREIGN KEY fk_platform_features_microservice,
 ADD CONSTRAINT fk_service_context_features_microservice FOREIGN KEY (microservice_id) REFERENCES service_context_microservices(id);
ALTER TABLE service_context_feature_groups
 DROP FOREIGN KEY fk_platform_feature_context_relations_feature,
 DROP FOREIGN KEY fk_platform_feature_context_relations_context,
 ADD CONSTRAINT fk_service_context_feature_groups_feature FOREIGN KEY (feature_id) REFERENCES service_context_features(id) ON DELETE CASCADE,
 ADD CONSTRAINT fk_service_context_feature_groups_group FOREIGN KEY (group_id) REFERENCES service_context_groups(id);
ALTER TABLE messages DROP FOREIGN KEY fk_messages_platform_microservice,
 ADD CONSTRAINT fk_messages_service_context_microservice FOREIGN KEY (microservice_id) REFERENCES service_context_microservices(id);
CREATE VIEW vw_service_context_feature_runtime_config AS
SELECT f.code                                                                                 AS feature_code,
       f.name                                                                                 AS feature_label,
       s.code                                                                                 AS microservice_code,
       s.name                                                                                 AS microservice_label,
       JSON_EXTRACT(f.settings, '$.quarantine.enabled') = true                                AS quarantine_enabled,
       CAST(JSON_UNQUOTE(JSON_EXTRACT(f.settings, '$.quarantine.retentionDays')) AS UNSIGNED) AS quarantine_retention_days,
       JSON_EXTRACT(f.settings, '$.quarantine.restoreAllowed') = true                         AS quarantine_restore_allowed,
       JSON_EXTRACT(f.settings, '$.audit.enabled') = true                                     AS audit_enabled,
       JSON_EXTRACT(f.settings, '$.audit.snapshotOnPurge') = true                             AS audit_snapshot_on_purge,
       JSON_EXTRACT(f.settings, '$.purge.enabled') = true                                     AS purge_enabled,
       f.lifecycle_code = 'ACTIVE'                                                            AS feature_active,
       s.lifecycle_code = 'ACTIVE'                                                            AS microservice_active
FROM service_context_features f
         JOIN service_context_microservices s ON s.id = f.microservice_id;


CREATE VIEW vw_dynamic_bundle_messages AS
SELECT CONCAT(s.code, '.', m.message_key) AS message_key,
 m.code AS code, m.http_status AS http_status, mt.locale AS locale,
 mt.title AS title, mt.detail AS message, mt.suggestion AS solution
FROM messages m JOIN service_context_microservices s ON s.id=m.microservice_id
JOIN message_translators mt ON mt.message_id=m.id
WHERE s.lifecycle_code='ACTIVE' AND m.lifecycle_code='ACTIVE' AND mt.lifecycle_code='ACTIVE';
UPDATE schema_definitions SET code='group-resource', name='Group Resource' WHERE scope_code='PLATFORM' AND code='context-resource';
UPDATE schema_configuration SET resource_type='SERVICE_CONTEXT', resource_code=CASE WHEN resource_code='context' THEN 'group' ELSE resource_code END WHERE resource_type='PLATFORM' AND resource_code IN ('microservice','feature','context');
INSERT INTO schema_versions(identifier,schema_id,schema_version,version_name,definition,status,created_at) SELECT UUID(),sd.id,v.latest+1,'Group Resource v3','{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Group request",
    "type": "object",
    "additionalProperties": false,
    "required": [
        "code",
        "name"
    ],
    "properties": {
        "version": {
            "type": [
                "integer",
                "null"
            ],
            "minimum": 0,
            "maximum": 9223372036854775807
        },
        "code": {
            "type": "string",
            "maxLength": 50,
            "pattern": "^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$"
        },
        "name": {
            "type": "string",
            "minLength": 1,
            "maxLength": 100,
            "pattern": "\\\\S"
        },
        "description": {
            "type": [
                "string",
                "null"
            ],
            "maxLength": 500
        }
    }
}
','PUBLISHED',CURRENT_TIMESTAMP FROM schema_definitions sd JOIN (SELECT schema_id,MAX(schema_version) latest FROM schema_versions GROUP BY schema_id) v ON v.schema_id=sd.id WHERE sd.scope_code='PLATFORM' AND sd.code='group-resource';


-- Current schema statements sourced from V79__segmentation_names.sql
-- Existing mandatory FK and per-segment unique constraint already prohibit orphans and duplicates.
-- RENAME preserves every record; no cleanup or deletion is performed.
RENAME TABLE segment_category TO category;
ALTER TABLE category
 RENAME INDEX uk_segment_category_identifier TO uk_category_identifier,
 RENAME INDEX uk_segment_category_code TO uk_category_code,
 RENAME INDEX fk_segment_category_lifecycle TO fk_category_lifecycle,
 DROP FOREIGN KEY fk_segment_category_lifecycle,
 DROP FOREIGN KEY fk_segment_category_segment,
 DROP CHECK ck_segment_category_code,
 DROP CHECK ck_segment_category_name,
 DROP CHECK ck_segment_category_lifecycle,
 ADD CONSTRAINT fk_category_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code),
 ADD CONSTRAINT fk_category_segment FOREIGN KEY (segment_id) REFERENCES segment(id),
 ADD CONSTRAINT ck_category_code CHECK (REGEXP_LIKE(code,'^[a-z0-9]+(-[a-z0-9]+)*$','c')),
 ADD CONSTRAINT ck_category_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),
 ADD CONSTRAINT ck_category_lifecycle CHECK (lifecycle_code IN ('ACTIVE','INACTIVE'));
UPDATE schema_definitions SET code='category-resource', name='Category request' WHERE scope_code='PLATFORM' AND code='segment-category-resource';
UPDATE schema_configuration SET resource_type='SEGMENTATION', resource_code=CASE WHEN resource_code='segment-category' THEN 'category' ELSE resource_code END WHERE resource_type='PLATFORM' AND resource_code IN ('segment','segment-category');
INSERT INTO schema_versions(identifier,schema_id,schema_version,version_name,definition,status,created_at) SELECT UUID(),sd.id,v.latest+1,'Category Resource v2','{
    "$schema": "https://json-schema.org/draft/2020-12/schema",
    "title": "Segment Category request",
    "type": "object",
    "additionalProperties": false,
    "required": [
        "code",
        "name"
    ],
    "properties": {
        "version": {
            "type": [
                "integer",
                "null"
            ],
            "minimum": 0,
            "maximum": 9223372036854775807
        },
        "code": {
            "type": "string",
            "maxLength": 100,
            "pattern": "^[a-z0-9]+(?:-[a-z0-9]+)*$"
        },
        "name": {
            "type": "string",
            "minLength": 1,
            "maxLength": 150,
            "pattern": "\\\\S"
        }
    }
}
','PUBLISHED',CURRENT_TIMESTAMP FROM schema_definitions sd JOIN (SELECT schema_id,MAX(schema_version) latest FROM schema_versions GROUP BY schema_id) v ON v.schema_id=sd.id WHERE sd.scope_code='PLATFORM' AND sd.code='category-resource';


-- Current schema statements sourced from V80__resource_sharing_names.sql
-- Rename aggregates and ownership column; preserve all data and FK relationships.
RENAME TABLE shared_contracts TO resource_sharing_contracts,
 shared_participations TO resource_sharing_participants,
 shared_environment_mappings TO resource_sharing_environment_mappings;
ALTER TABLE resource_sharing_environment_mappings RENAME COLUMN participation_id TO participant_id;
ALTER TABLE resource_sharing_contracts RENAME INDEX uk_shared_contracts_identifier TO uk_resource_sharing_contracts_identifier;
ALTER TABLE resource_sharing_contracts RENAME INDEX uk_shared_contracts_scope_feature TO uk_resource_sharing_contracts_scope_feature;
ALTER TABLE resource_sharing_contracts RENAME INDEX idx_shared_contracts_owner_scope TO idx_resource_sharing_contracts_owner_scope;
ALTER TABLE resource_sharing_contracts RENAME INDEX idx_shared_contracts_feature TO idx_resource_sharing_contracts_feature;
ALTER TABLE resource_sharing_contracts RENAME INDEX fk_shared_contracts_application TO fk_resource_sharing_contracts_application;
ALTER TABLE resource_sharing_participants RENAME INDEX uk_shared_participations_identifier TO uk_resource_sharing_participants_identifier;
ALTER TABLE resource_sharing_participants RENAME INDEX uk_shared_participations_contract_application TO uk_resource_sharing_participants_contract_application;
ALTER TABLE resource_sharing_participants RENAME INDEX idx_shared_participations_participant TO idx_resource_sharing_participants_participant;
ALTER TABLE resource_sharing_participants RENAME INDEX fk_shared_participations_status TO fk_resource_sharing_participants_status;
ALTER TABLE resource_sharing_participants RENAME INDEX fk_shared_participations_publication_mode TO fk_resource_sharing_participants_publication_mode;
ALTER TABLE resource_sharing_participants RENAME INDEX fk_shared_participations_application TO fk_resource_sharing_participants_application;
ALTER TABLE resource_sharing_environment_mappings RENAME INDEX uk_shared_mapping_destination TO uk_resource_sharing_mapping_destination;
ALTER TABLE resource_sharing_environment_mappings RENAME INDEX fk_shared_mapping_source_environment TO fk_resource_sharing_mapping_source_environment;
ALTER TABLE resource_sharing_environment_mappings RENAME INDEX fk_shared_mapping_destination_environment TO fk_resource_sharing_mapping_destination_environment;
ALTER TABLE resource_sharing_contracts
 DROP FOREIGN KEY fk_shared_contracts_workspace,
 ADD CONSTRAINT fk_resource_sharing_contracts_workspace FOREIGN KEY (owner_workspace_id) REFERENCES workspaces(id),
 DROP FOREIGN KEY fk_shared_contracts_application,
 ADD CONSTRAINT fk_resource_sharing_contracts_application FOREIGN KEY (owner_application_id) REFERENCES applications(id),
 DROP FOREIGN KEY fk_shared_contracts_feature,
 ADD CONSTRAINT fk_resource_sharing_contracts_feature FOREIGN KEY (feature_id) REFERENCES service_context_features(id);
ALTER TABLE resource_sharing_participants
 DROP FOREIGN KEY fk_shared_participations_contract,
 ADD CONSTRAINT fk_resource_sharing_participants_contract FOREIGN KEY (contract_id) REFERENCES resource_sharing_contracts(id) ON DELETE CASCADE,
 DROP FOREIGN KEY fk_shared_participations_status,
 ADD CONSTRAINT fk_resource_sharing_participants_status FOREIGN KEY (status_code) REFERENCES type_sharing_statuses(code),
 DROP FOREIGN KEY fk_shared_participations_publication_mode,
 ADD CONSTRAINT fk_resource_sharing_participants_publication_mode FOREIGN KEY (publication_mode_code) REFERENCES type_publication_modes(code),
 DROP FOREIGN KEY fk_shared_participations_workspace,
 ADD CONSTRAINT fk_resource_sharing_participants_workspace FOREIGN KEY (participant_workspace_id) REFERENCES workspaces(id),
 DROP FOREIGN KEY fk_shared_participations_application,
 ADD CONSTRAINT fk_resource_sharing_participants_application FOREIGN KEY (participant_application_id) REFERENCES applications(id);
ALTER TABLE resource_sharing_environment_mappings
 DROP FOREIGN KEY fk_shared_mapping_participation,
 ADD CONSTRAINT fk_resource_sharing_mapping_participant FOREIGN KEY (participant_id) REFERENCES resource_sharing_participants(id) ON DELETE CASCADE,
 DROP FOREIGN KEY fk_shared_mapping_source_environment,
 ADD CONSTRAINT fk_resource_sharing_mapping_source_environment FOREIGN KEY (source_environment_id) REFERENCES environments(id),
 DROP FOREIGN KEY fk_shared_mapping_destination_environment,
 ADD CONSTRAINT fk_resource_sharing_mapping_destination_environment FOREIGN KEY (destination_environment_id) REFERENCES environments(id);


-- Current schema statements sourced from V81__feature_group_names.sql
-- Rename the Service Context group aggregate and its N:N join table without recreating data.
RENAME TABLE service_context_groups TO feature_group,
             service_context_feature_groups TO feature_group_feature;
ALTER TABLE feature_group_feature RENAME COLUMN group_id TO feature_group_id;

ALTER TABLE feature_group
    DROP FOREIGN KEY fk_service_context_groups_lifecycle,
    DROP CHECK ck_service_context_groups_code;
ALTER TABLE feature_group_feature
    DROP FOREIGN KEY fk_service_context_feature_groups_feature,
    DROP FOREIGN KEY fk_service_context_feature_groups_group;

ALTER TABLE feature_group
    RENAME INDEX fk_service_context_groups_lifecycle TO fk_feature_group_lifecycle,
    RENAME INDEX uk_service_context_groups_identifier TO uk_feature_group_identifier,
    RENAME INDEX uk_service_context_groups_code TO uk_feature_group_code,
    RENAME INDEX uk_service_context_groups_name TO uk_feature_group_name;
ALTER TABLE feature_group_feature
    RENAME INDEX fk_service_context_feature_groups_group TO fk_feature_group_feature_feature_group;

ALTER TABLE feature_group
    ADD CONSTRAINT fk_feature_group_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code),
    ADD CONSTRAINT ck_feature_group_code CHECK (code REGEXP '^[a-z][a-z0-9]*(-[a-z0-9]+)*$');
ALTER TABLE feature_group_feature
    ADD CONSTRAINT fk_feature_group_feature_feature FOREIGN KEY (feature_id) REFERENCES service_context_features(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_feature_group_feature_feature_group FOREIGN KEY (feature_group_id) REFERENCES feature_group(id);

UPDATE schema_definitions
SET code = 'feature-group-resource', name = 'Feature Group Resource'
WHERE scope_code = 'PLATFORM' AND code = 'group-resource';
UPDATE schema_configuration
SET resource_code = 'feature-group'
WHERE resource_type = 'SERVICE_CONTEXT' AND resource_code = 'group';

INSERT INTO schema_versions(identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), definition.id, latest.schema_version + 1,
       CONCAT('Feature Group Resource v', latest.schema_version + 1),
       JSON_SET(published.definition, '$.title', 'Feature Group request'),
       'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions definition
JOIN (
    SELECT schema_id, MAX(schema_version) AS schema_version
    FROM schema_versions
    WHERE status = 'PUBLISHED'
    GROUP BY schema_id
) latest ON latest.schema_id = definition.id
JOIN schema_versions published
  ON published.schema_id = latest.schema_id
 AND published.schema_version = latest.schema_version
WHERE definition.scope_code = 'PLATFORM'
  AND definition.code = 'feature-group-resource';


-- Current schema statements sourced from V82__canonical_feature_group_and_category_schema_names.sql
-- Keep immutable historical schema versions while publishing their canonical domain titles.
UPDATE schema_definitions
SET code = 'feature-group-feature-resource', name = 'Feature Group Feature Resource'
WHERE scope_code = 'PLATFORM' AND code = 'feature-context-resource';

UPDATE schema_configuration
SET resource_type = 'SERVICE_CONTEXT', resource_code = 'feature-group-feature'
WHERE resource_type = 'PLATFORM' AND resource_code = 'feature-context';

INSERT INTO schema_versions(identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), definition.id, latest.schema_version + 1,
       CONCAT('Feature Group Feature Resource v', latest.schema_version + 1),
       JSON_SET(published.definition, '$.title', 'Feature Group associations request'),
       'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions definition
JOIN (
    SELECT schema_id, MAX(schema_version) AS schema_version
    FROM schema_versions
    WHERE status = 'PUBLISHED'
    GROUP BY schema_id
) latest ON latest.schema_id = definition.id
JOIN schema_versions published
  ON published.schema_id = latest.schema_id
 AND published.schema_version = latest.schema_version
WHERE definition.scope_code = 'PLATFORM'
  AND definition.code = 'feature-group-feature-resource';

INSERT INTO schema_versions(identifier, schema_id, schema_version, version_name, definition, status, created_at)
SELECT UUID(), definition.id, latest.schema_version + 1,
       CONCAT('Category Resource v', latest.schema_version + 1),
       JSON_SET(published.definition, '$.title', 'Category request'),
       'PUBLISHED', CURRENT_TIMESTAMP
FROM schema_definitions definition
JOIN (
    SELECT schema_id, MAX(schema_version) AS schema_version
    FROM schema_versions
    WHERE status = 'PUBLISHED'
    GROUP BY schema_id
) latest ON latest.schema_id = definition.id
JOIN schema_versions published
  ON published.schema_id = latest.schema_id
 AND published.schema_version = latest.schema_version
WHERE definition.scope_code = 'PLATFORM'
  AND definition.code = 'category-resource';


-- Current schema statements sourced from V83__rename_schema_configuration_to_binding.sql
-- Rename the schema-to-resource association in place; all rows and identifiers are preserved.
RENAME TABLE schema_configuration TO schema_binding;

ALTER TABLE schema_binding
    RENAME INDEX uk_schema_configuration_identifier TO uk_schema_binding_identifier,
    RENAME INDEX uk_schema_configuration_resource TO uk_schema_binding_resource;

ALTER TABLE schema_binding
    DROP FOREIGN KEY fk_schema_configuration_schema,
    DROP FOREIGN KEY fk_schema_configuration_lifecycle;

ALTER TABLE schema_binding
    ADD CONSTRAINT fk_schema_binding_schema FOREIGN KEY (schema_id) REFERENCES schema_definitions(id),
    ADD CONSTRAINT fk_schema_binding_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code);

CREATE OR REPLACE VIEW vw_platform_resource_schemas AS
SELECT sb.resource_type,
       sb.resource_code,
       sv.schema_version,
       sv.definition
FROM schema_binding sb
JOIN schema_definitions sd
  ON sd.id = sb.schema_id
JOIN schema_versions sv
  ON sv.schema_id = sd.id
WHERE sb.lifecycle_code = 'ACTIVE'
  AND sd.lifecycle_code = 'ACTIVE'
  AND sv.status = 'PUBLISHED'
  AND sv.schema_version = (
      SELECT MAX(sv2.schema_version)
      FROM schema_versions sv2
      WHERE sv2.schema_id = sd.id
        AND sv2.status = 'PUBLISHED'
  );


-- Current schema statements sourced from V84__create_environment_configurations.sql
-- Independent environment settings; existing registration settings and dynamic parameters are unchanged.

CREATE TABLE workspace_environment_configurations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL,
    workspace_id BIGINT NOT NULL,
    environment_id BIGINT NOT NULL,
    lifecycle_code VARCHAR(50) NOT NULL,
    settings JSON NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_workspace_env_config PRIMARY KEY (id),
    CONSTRAINT uk_workspace_env_config_identifier UNIQUE (identifier),
    CONSTRAINT uk_workspace_env_config_owner_environment UNIQUE (workspace_id, environment_id),
    CONSTRAINT fk_workspace_env_config_owner FOREIGN KEY (workspace_id) REFERENCES workspaces(id),
    CONSTRAINT fk_workspace_env_config_environment FOREIGN KEY (environment_id) REFERENCES environments(id),
    CONSTRAINT fk_workspace_env_config_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code)
);
CREATE INDEX idx_workspace_env_config_lifecycle ON workspace_environment_configurations (workspace_id, lifecycle_code, id);

CREATE TABLE workspace_environment_configuration_publishers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    configuration_id BIGINT NOT NULL,
    publisher_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    parameters JSON NOT NULL,
    CONSTRAINT pk_workspace_env_config_publisher PRIMARY KEY (id),
    CONSTRAINT uk_workspace_env_config_publisher UNIQUE (configuration_id, publisher_id),
    CONSTRAINT uk_workspace_env_config_order UNIQUE (configuration_id, sort_order),
    CONSTRAINT fk_workspace_env_config_publisher_owner FOREIGN KEY (configuration_id) REFERENCES workspace_environment_configurations(id),
    CONSTRAINT fk_workspace_env_config_publisher_reference FOREIGN KEY (publisher_id) REFERENCES publishers(id),
    CONSTRAINT ck_workspace_env_config_order CHECK (sort_order > 0)
);

CREATE TABLE application_environment_configurations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL,
    application_id BIGINT NOT NULL,
    environment_id BIGINT NOT NULL,
    lifecycle_code VARCHAR(50) NOT NULL,
    settings JSON NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_application_env_config PRIMARY KEY (id),
    CONSTRAINT uk_application_env_config_identifier UNIQUE (identifier),
    CONSTRAINT uk_application_env_config_owner_environment UNIQUE (application_id, environment_id),
    CONSTRAINT fk_application_env_config_owner FOREIGN KEY (application_id) REFERENCES applications(id),
    CONSTRAINT fk_application_env_config_environment FOREIGN KEY (environment_id) REFERENCES environments(id),
    CONSTRAINT fk_application_env_config_lifecycle FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code)
);
CREATE INDEX idx_application_env_config_lifecycle ON application_environment_configurations (application_id, lifecycle_code, id);

CREATE TABLE application_environment_configuration_publishers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    configuration_id BIGINT NOT NULL,
    publisher_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    parameters JSON NOT NULL,
    CONSTRAINT pk_application_env_config_publisher PRIMARY KEY (id),
    CONSTRAINT uk_application_env_config_publisher UNIQUE (configuration_id, publisher_id),
    CONSTRAINT uk_application_env_config_order UNIQUE (configuration_id, sort_order),
    CONSTRAINT fk_application_env_config_publisher_owner FOREIGN KEY (configuration_id) REFERENCES application_environment_configurations(id),
    CONSTRAINT fk_application_env_config_publisher_reference FOREIGN KEY (publisher_id) REFERENCES publishers(id),
    CONSTRAINT ck_application_env_config_order CHECK (sort_order > 0)
);


-- Current schema statements sourced from V85__environment_configuration_schemas.sql
-- Dedicated environment configuration contracts. Settings/parameters are free JSON objects, as approved.
INSERT IGNORE INTO schema_definitions (version,identifier,scope_code,code,name,lifecycle_code,created_at,updated_at) VALUES (0,UUID(),'PLATFORM','workspace-environment-configuration-create','workspace-environment-configuration-create','ACTIVE',NOW(),NOW());
INSERT IGNORE INTO schema_versions (identifier,schema_id,schema_version,version_name,definition,status,created_at) SELECT UUID(),id,1,'workspace-environment-configuration-create v1','{"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object","additionalProperties":false,"required":["settings","environmentIdentifier","publishers"],"properties":{"version":{"type":"null"},"settings":{"type":"object","additionalProperties":true},"publishers":{"type":"array","minItems":1,"items":{"type":"object","additionalProperties":false,"required":["publisherIdentifier","sortOrder","parameters"],"properties":{"publisherIdentifier":{"type":"string","format":"uuid"},"sortOrder":{"type":"integer","minimum":1,"maximum":2147483647},"parameters":{"type":"object","additionalProperties":true}}}},"environmentIdentifier":{"type":"string","format":"uuid"}}}','PUBLISHED',NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='workspace-environment-configuration-create';
INSERT IGNORE INTO schema_binding (version,identifier,resource_type,resource_code,schema_id,lifecycle_code,created_at,updated_at) SELECT 0,UUID(),'PLATFORM','workspace-environment-configuration-create',id,'ACTIVE',NOW(),NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='workspace-environment-configuration-create';
INSERT IGNORE INTO schema_definitions (version,identifier,scope_code,code,name,lifecycle_code,created_at,updated_at) VALUES (0,UUID(),'PLATFORM','workspace-environment-configuration-update','workspace-environment-configuration-update','ACTIVE',NOW(),NOW());
INSERT IGNORE INTO schema_versions (identifier,schema_id,schema_version,version_name,definition,status,created_at) SELECT UUID(),id,1,'workspace-environment-configuration-update v1','{"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object","additionalProperties":false,"required":["settings","version","publishers"],"properties":{"version":{"type":"integer","minimum":0,"maximum":9223372036854775807},"settings":{"type":"object","additionalProperties":true},"publishers":{"type":"array","minItems":1,"items":{"type":"object","additionalProperties":false,"required":["publisherIdentifier","sortOrder","parameters"],"properties":{"publisherIdentifier":{"type":"string","format":"uuid"},"sortOrder":{"type":"integer","minimum":1,"maximum":2147483647},"parameters":{"type":"object","additionalProperties":true}}}}}}','PUBLISHED',NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='workspace-environment-configuration-update';
INSERT IGNORE INTO schema_binding (version,identifier,resource_type,resource_code,schema_id,lifecycle_code,created_at,updated_at) SELECT 0,UUID(),'PLATFORM','workspace-environment-configuration-update',id,'ACTIVE',NOW(),NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='workspace-environment-configuration-update';
INSERT IGNORE INTO schema_definitions (version,identifier,scope_code,code,name,lifecycle_code,created_at,updated_at) VALUES (0,UUID(),'PLATFORM','workspace-environment-configuration-settings','workspace-environment-configuration-settings','ACTIVE',NOW(),NOW());
INSERT IGNORE INTO schema_versions (identifier,schema_id,schema_version,version_name,definition,status,created_at) SELECT UUID(),id,1,'workspace-environment-configuration-settings v1','{"type":"object","additionalProperties":true}','PUBLISHED',NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='workspace-environment-configuration-settings';
INSERT IGNORE INTO schema_binding (version,identifier,resource_type,resource_code,schema_id,lifecycle_code,created_at,updated_at) SELECT 0,UUID(),'PLATFORM','workspace-environment-configuration-settings',id,'ACTIVE',NOW(),NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='workspace-environment-configuration-settings';
INSERT IGNORE INTO schema_definitions (version,identifier,scope_code,code,name,lifecycle_code,created_at,updated_at) VALUES (0,UUID(),'PLATFORM','application-environment-configuration-create','application-environment-configuration-create','ACTIVE',NOW(),NOW());
INSERT IGNORE INTO schema_versions (identifier,schema_id,schema_version,version_name,definition,status,created_at) SELECT UUID(),id,1,'application-environment-configuration-create v1','{"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object","additionalProperties":false,"required":["settings","environmentIdentifier"],"properties":{"version":{"type":"null"},"settings":{"type":"object","additionalProperties":true},"publishers":{"type":"array","minItems":1,"items":{"type":"object","additionalProperties":false,"required":["publisherIdentifier","sortOrder","parameters"],"properties":{"publisherIdentifier":{"type":"string","format":"uuid"},"sortOrder":{"type":"integer","minimum":1,"maximum":2147483647},"parameters":{"type":"object","additionalProperties":true}}}},"environmentIdentifier":{"type":"string","format":"uuid"},"copyWorkspacePublishers":{"type":"boolean","default":false}},"if":{"required":["copyWorkspacePublishers"],"properties":{"copyWorkspacePublishers":{"const":true}}},"then":{"not":{"required":["publishers"]}},"else":{"required":["publishers"]}}','PUBLISHED',NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='application-environment-configuration-create';
INSERT IGNORE INTO schema_binding (version,identifier,resource_type,resource_code,schema_id,lifecycle_code,created_at,updated_at) SELECT 0,UUID(),'PLATFORM','application-environment-configuration-create',id,'ACTIVE',NOW(),NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='application-environment-configuration-create';
INSERT IGNORE INTO schema_definitions (version,identifier,scope_code,code,name,lifecycle_code,created_at,updated_at) VALUES (0,UUID(),'PLATFORM','application-environment-configuration-update','application-environment-configuration-update','ACTIVE',NOW(),NOW());
INSERT IGNORE INTO schema_versions (identifier,schema_id,schema_version,version_name,definition,status,created_at) SELECT UUID(),id,1,'application-environment-configuration-update v1','{"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object","additionalProperties":false,"required":["settings","version"],"properties":{"version":{"type":"integer","minimum":0,"maximum":9223372036854775807},"settings":{"type":"object","additionalProperties":true},"publishers":{"type":"array","minItems":1,"items":{"type":"object","additionalProperties":false,"required":["publisherIdentifier","sortOrder","parameters"],"properties":{"publisherIdentifier":{"type":"string","format":"uuid"},"sortOrder":{"type":"integer","minimum":1,"maximum":2147483647},"parameters":{"type":"object","additionalProperties":true}}}},"copyWorkspacePublishers":{"type":"boolean","default":false}},"if":{"required":["copyWorkspacePublishers"],"properties":{"copyWorkspacePublishers":{"const":true}}},"then":{"not":{"required":["publishers"]}},"else":{"required":["publishers"]}}','PUBLISHED',NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='application-environment-configuration-update';
INSERT IGNORE INTO schema_binding (version,identifier,resource_type,resource_code,schema_id,lifecycle_code,created_at,updated_at) SELECT 0,UUID(),'PLATFORM','application-environment-configuration-update',id,'ACTIVE',NOW(),NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='application-environment-configuration-update';
INSERT IGNORE INTO schema_definitions (version,identifier,scope_code,code,name,lifecycle_code,created_at,updated_at) VALUES (0,UUID(),'PLATFORM','application-environment-configuration-settings','application-environment-configuration-settings','ACTIVE',NOW(),NOW());
INSERT IGNORE INTO schema_versions (identifier,schema_id,schema_version,version_name,definition,status,created_at) SELECT UUID(),id,1,'application-environment-configuration-settings v1','{"type":"object","additionalProperties":true}','PUBLISHED',NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='application-environment-configuration-settings';
INSERT IGNORE INTO schema_binding (version,identifier,resource_type,resource_code,schema_id,lifecycle_code,created_at,updated_at) SELECT 0,UUID(),'PLATFORM','application-environment-configuration-settings',id,'ACTIVE',NOW(),NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='application-environment-configuration-settings';
INSERT IGNORE INTO schema_definitions (version,identifier,scope_code,code,name,lifecycle_code,created_at,updated_at) VALUES (0,UUID(),'PLATFORM','environment-configuration-publisher-parameters','environment-configuration-publisher-parameters','ACTIVE',NOW(),NOW());
INSERT IGNORE INTO schema_versions (identifier,schema_id,schema_version,version_name,definition,status,created_at) SELECT UUID(),id,1,'environment-configuration-publisher-parameters v1','{"type":"object","additionalProperties":true}','PUBLISHED',NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='environment-configuration-publisher-parameters';
INSERT IGNORE INTO schema_binding (version,identifier,resource_type,resource_code,schema_id,lifecycle_code,created_at,updated_at) SELECT 0,UUID(),'PLATFORM','environment-configuration-publisher-parameters',id,'ACTIVE',NOW(),NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='environment-configuration-publisher-parameters';


-- Current schema statements sourced from V86__configuration_publisher_contracts.sql
-- Rename technical codes only after checking collisions; association FKs use immutable ids.
-- SIGNAL guards execute before any persistent DML. Existing audit snapshots remain historical.
DELIMITER $$
CREATE PROCEDURE validate_publisher_code_rename_v86()
BEGIN
    IF EXISTS (SELECT 1 FROM publishers a JOIN publishers b ON a.code='HTTP' AND b.code='WEB_SOCKET')
       OR EXISTS (SELECT 1 FROM publishers a JOIN publishers b ON a.code='APPCONFIG' AND b.code='APP_CONFIG')
       OR EXISTS (SELECT 1 FROM schema_binding a JOIN schema_binding b ON a.resource_type='PUBLISHER' AND b.resource_type='PUBLISHER' AND a.resource_code='HTTP' AND b.resource_code='WEB_SOCKET')
       OR EXISTS (SELECT 1 FROM schema_binding a JOIN schema_binding b ON a.resource_type='PUBLISHER' AND b.resource_type='PUBLISHER' AND a.resource_code='APPCONFIG' AND b.resource_code='APP_CONFIG') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Publisher code rename collision; reconcile catalog before V86';
    END IF;
    IF EXISTS (
        SELECT 1 FROM schema_versions v JOIN schema_definitions d ON d.id=v.schema_id
        WHERE d.scope_code='PLATFORM' AND d.code='publisher-resource' AND v.status='PUBLISHED'
          AND v.schema_version=(SELECT MAX(p.schema_version) FROM schema_versions p WHERE p.schema_id=v.schema_id AND p.status='PUBLISHED')
          AND (COALESCE(JSON_UNQUOTE(JSON_EXTRACT(v.definition,'$.allOf[1].if.properties.code.const')),'') <> 'APPCONFIG'
            OR COALESCE(JSON_UNQUOTE(JSON_EXTRACT(v.definition,'$.allOf[2].if.properties.code.const')),'') <> 'HTTP')
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Publisher schema layout differs from baseline; review V86 mapping';
    END IF;
END$$
DELIMITER ;
CALL validate_publisher_code_rename_v86();
DROP PROCEDURE validate_publisher_code_rename_v86;
UPDATE publishers SET code = CASE code WHEN 'HTTP' THEN 'WEB_SOCKET' WHEN 'APPCONFIG' THEN 'APP_CONFIG' END,
    version=version+1, updated_at=NOW(6) WHERE code IN ('HTTP','APPCONFIG');
-- Preserve the ids and schema ownership of existing per-publisher settings bindings.
UPDATE schema_binding SET resource_code=CASE resource_code WHEN 'HTTP' THEN 'WEB_SOCKET' WHEN 'APPCONFIG' THEN 'APP_CONFIG' END,
 version=version+1,updated_at=NOW() WHERE resource_type='PUBLISHER' AND resource_code IN ('HTTP','APPCONFIG');
-- Publish a successor of the latest cadastral schema, preserving administrated restrictions.
INSERT INTO schema_versions (identifier,schema_id,schema_version,version_name,definition,status,created_at)
SELECT UUID(),v.schema_id, (SELECT COALESCE(MAX(x.schema_version),0)+1 FROM schema_versions x WHERE x.schema_id=v.schema_id),
 'Publisher technical codes v86',
 JSON_SET(v.definition,
   '$.properties.code.enum',JSON_ARRAY('KAFKA','WEB_SOCKET','APP_CONFIG'),
   '$.allOf[1].if.properties.code.const','APP_CONFIG',
   '$.allOf[2].if.properties.code.const','WEB_SOCKET'),
 'PUBLISHED',NOW()
FROM schema_versions v JOIN schema_definitions d ON d.id=v.schema_id
WHERE d.scope_code='PLATFORM' AND d.code='publisher-resource' AND v.status='PUBLISHED'
 AND v.schema_version=(SELECT MAX(p.schema_version) FROM schema_versions p WHERE p.schema_id=v.schema_id AND p.status='PUBLISHED');

INSERT INTO schema_definitions (version,identifier,scope_code,code,name,lifecycle_code,created_at,updated_at)
VALUES (0,UUID(),'PLATFORM','workspace-environment-configuration','workspace-environment-configuration','ACTIVE',NOW(),NOW());
INSERT INTO schema_versions (identifier,schema_id,schema_version,version_name,definition,status,created_at)
SELECT UUID(),id,1,'workspace-environment-configuration v1','{"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object","additionalProperties":false,"required":["settings","publishers"],"properties":{"version":{"type":["integer","null"],"minimum":0,"maximum":9223372036854775807},"settings":{"type":"object","additionalProperties":true},"publishers":{"type":"array","minItems":1,"items":{"type":"object","additionalProperties":false,"required":["publisherIdentifier","sortOrder","parameters"],"properties":{"publisherIdentifier":{"type":"string","format":"uuid"},"sortOrder":{"type":"integer","minimum":1,"maximum":2147483647},"parameters":{"type":"object","additionalProperties":true}}}},"environmentIdentifier":{"type":"string","format":"uuid"}},"if":{"required":["version"],"properties":{"version":{"type":"integer"}}},"then":{"not":{"required":["environmentIdentifier"]}},"else":{"required":["environmentIdentifier"]}}','PUBLISHED',NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='workspace-environment-configuration';
INSERT INTO schema_binding (version,identifier,resource_type,resource_code,schema_id,lifecycle_code,created_at,updated_at)
SELECT 0,UUID(),'PLATFORM','workspace-environment-configuration',id,'ACTIVE',NOW(),NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='workspace-environment-configuration';

INSERT INTO schema_definitions (version,identifier,scope_code,code,name,lifecycle_code,created_at,updated_at)
VALUES (0,UUID(),'PLATFORM','application-environment-configuration','application-environment-configuration','ACTIVE',NOW(),NOW());
INSERT INTO schema_versions (identifier,schema_id,schema_version,version_name,definition,status,created_at)
SELECT UUID(),id,1,'application-environment-configuration v1','{"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object","additionalProperties":false,"required":["settings"],"properties":{"version":{"type":["integer","null"],"minimum":0,"maximum":9223372036854775807},"settings":{"type":"object","additionalProperties":true},"publishers":{"type":"array","minItems":1,"items":{"type":"object","additionalProperties":false,"required":["publisherIdentifier","sortOrder","parameters"],"properties":{"publisherIdentifier":{"type":"string","format":"uuid"},"sortOrder":{"type":"integer","minimum":1,"maximum":2147483647},"parameters":{"type":"object","additionalProperties":true}}}},"environmentIdentifier":{"type":"string","format":"uuid"},"copyWorkspacePublishers":{"type":"boolean","default":false}},"allOf":[{"if":{"required":["version"],"properties":{"version":{"type":"integer"}}},"then":{"not":{"required":["environmentIdentifier"]}},"else":{"required":["environmentIdentifier"]}},{"if":{"required":["copyWorkspacePublishers"],"properties":{"copyWorkspacePublishers":{"const":true}}},"then":{"not":{"required":["publishers"]}},"else":{"required":["publishers"]}}]}','PUBLISHED',NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='application-environment-configuration';
INSERT INTO schema_binding (version,identifier,resource_type,resource_code,schema_id,lifecycle_code,created_at,updated_at)
SELECT 0,UUID(),'PLATFORM','application-environment-configuration',id,'ACTIVE',NOW(),NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='application-environment-configuration';

INSERT INTO schema_definitions (version,identifier,scope_code,code,name,lifecycle_code,created_at,updated_at)
VALUES (0,UUID(),'PLATFORM','environment-configuration-publisher-web-socket','environment-configuration-publisher-web-socket','ACTIVE',NOW(),NOW());
INSERT INTO schema_versions (identifier,schema_id,schema_version,version_name,definition,status,created_at)
SELECT UUID(),id,1,'environment-configuration-publisher-web-socket v1','{"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object","additionalProperties":false,"required":["publisherIdentifier","sortOrder","parameters"],"properties":{"publisherIdentifier":{"type":"string","format":"uuid","pattern":"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"},"sortOrder":{"type":"integer","minimum":1,"maximum":2147483647},"parameters":{"type":"object","additionalProperties":true}}}','PUBLISHED',NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='environment-configuration-publisher-web-socket';
INSERT INTO schema_binding (version,identifier,resource_type,resource_code,schema_id,lifecycle_code,created_at,updated_at)
SELECT 0,UUID(),'PLATFORM','environment-configuration-publisher-web-socket',id,'ACTIVE',NOW(),NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='environment-configuration-publisher-web-socket';

INSERT INTO schema_definitions (version,identifier,scope_code,code,name,lifecycle_code,created_at,updated_at)
VALUES (0,UUID(),'PLATFORM','environment-configuration-publisher-kafka','environment-configuration-publisher-kafka','ACTIVE',NOW(),NOW());
INSERT INTO schema_versions (identifier,schema_id,schema_version,version_name,definition,status,created_at)
SELECT UUID(),id,1,'environment-configuration-publisher-kafka v1','{"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object","additionalProperties":false,"required":["publisherIdentifier","sortOrder","parameters"],"properties":{"publisherIdentifier":{"type":"string","format":"uuid","pattern":"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"},"sortOrder":{"type":"integer","minimum":1,"maximum":2147483647},"parameters":{"type":"object","additionalProperties":true}}}','PUBLISHED',NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='environment-configuration-publisher-kafka';
INSERT INTO schema_binding (version,identifier,resource_type,resource_code,schema_id,lifecycle_code,created_at,updated_at)
SELECT 0,UUID(),'PLATFORM','environment-configuration-publisher-kafka',id,'ACTIVE',NOW(),NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='environment-configuration-publisher-kafka';

INSERT INTO schema_definitions (version,identifier,scope_code,code,name,lifecycle_code,created_at,updated_at)
VALUES (0,UUID(),'PLATFORM','environment-configuration-publisher-app-config','environment-configuration-publisher-app-config','ACTIVE',NOW(),NOW());
INSERT INTO schema_versions (identifier,schema_id,schema_version,version_name,definition,status,created_at)
SELECT UUID(),id,1,'environment-configuration-publisher-app-config v1','{"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object","additionalProperties":false,"required":["publisherIdentifier","sortOrder","parameters"],"properties":{"publisherIdentifier":{"type":"string","format":"uuid","pattern":"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"},"sortOrder":{"type":"integer","minimum":1,"maximum":2147483647},"parameters":{"type":"object","additionalProperties":true}}}','PUBLISHED',NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='environment-configuration-publisher-app-config';
INSERT INTO schema_binding (version,identifier,resource_type,resource_code,schema_id,lifecycle_code,created_at,updated_at)
SELECT 0,UUID(),'PLATFORM','environment-configuration-publisher-app-config',id,'ACTIVE',NOW(),NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='environment-configuration-publisher-app-config';


-- Current schema statements sourced from V87__publisher_catalog.sql
-- Generic catalog cutover. Published V1-V86 are unchanged.
-- Legacy identity/metadata remain in their columns, nullable and outside CatalogDTO/JPA.
DELIMITER $$
CREATE PROCEDURE validate_publisher_catalog_v87()
BEGIN
 IF EXISTS (SELECT 1 FROM publishers WHERE code NOT IN ('WEB_SOCKET','KAFKA','APP_CONFIG')) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unknown publisher code; reconcile catalog before V87';
 END IF;
 IF EXISTS (SELECT 1 FROM publishers WHERE JSON_TYPE(settings)<>'OBJECT'
   OR (JSON_CONTAINS_PATH(settings,'one','$.scope') AND JSON_UNQUOTE(JSON_EXTRACT(settings,'$.scope'))<>publisher_scope)) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Publisher settings/scope conflict; reconcile before V87';
 END IF;
END$$
DELIMITER ;
CALL validate_publisher_catalog_v87();
DROP PROCEDURE validate_publisher_catalog_v87;
ALTER TABLE publishers ADD COLUMN label VARCHAR(100) NULL, ADD COLUMN sort_order INT NULL, ADD COLUMN is_active BOOLEAN NULL;
UPDATE publishers SET label=name,sort_order=id,is_active=(lifecycle_code='ACTIVE'),settings=JSON_SET(settings,'$.scope',publisher_scope);
ALTER TABLE workspace_environment_configuration_publishers ADD COLUMN publisher_code VARCHAR(50) NULL;
UPDATE workspace_environment_configuration_publishers a JOIN publishers p ON p.id=a.publisher_id SET a.publisher_code=p.code;
ALTER TABLE workspace_environment_configuration_publishers
 DROP FOREIGN KEY fk_workspace_env_config_publisher_reference,
 DROP INDEX uk_workspace_env_config_publisher,
 MODIFY publisher_id BIGINT NULL,
 MODIFY publisher_code VARCHAR(50) NOT NULL,
 ADD CONSTRAINT uk_workspace_env_config_publisher UNIQUE(configuration_id,publisher_code),
 ADD CONSTRAINT fk_workspace_env_config_publisher_catalog FOREIGN KEY(publisher_code) REFERENCES publishers(code);
ALTER TABLE application_environment_configuration_publishers ADD COLUMN publisher_code VARCHAR(50) NULL;
UPDATE application_environment_configuration_publishers a JOIN publishers p ON p.id=a.publisher_id SET a.publisher_code=p.code;
ALTER TABLE application_environment_configuration_publishers
 DROP FOREIGN KEY fk_application_env_config_publisher_reference,
 DROP INDEX uk_application_env_config_publisher,
 MODIFY publisher_id BIGINT NULL,
 MODIFY publisher_code VARCHAR(50) NOT NULL,
 ADD CONSTRAINT uk_application_env_config_publisher UNIQUE(configuration_id,publisher_code),
 ADD CONSTRAINT fk_application_env_config_publisher_catalog FOREIGN KEY(publisher_code) REFERENCES publishers(code);
ALTER TABLE publishers
 DROP FOREIGN KEY fk_publishers_scope,
 ADD UNIQUE KEY uk_publishers_legacy_id(id),
 DROP PRIMARY KEY, ADD PRIMARY KEY(code),
 MODIFY label VARCHAR(100) NOT NULL, MODIFY sort_order INT NOT NULL, MODIFY is_active BOOLEAN NOT NULL,
 MODIFY identifier VARCHAR(36) NULL, MODIFY name VARCHAR(50) NULL, MODIFY publisher_scope VARCHAR(50) NULL,
 MODIFY lifecycle_code VARCHAR(50) NULL, MODIFY description TEXT NULL, MODIFY settings TEXT NOT NULL,
 MODIFY created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 MODIFY updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
INSERT INTO publishers(code,label,description,sort_order,is_active,settings)
SELECT 'WEB_SOCKET','WebSocket','Workspace publisher',1,TRUE,'{"scope":"WORKSPACE"}' WHERE NOT EXISTS(SELECT 1 FROM publishers WHERE code='WEB_SOCKET');
INSERT INTO publishers(code,label,description,sort_order,is_active,settings)
SELECT 'KAFKA','Kafka','Workspace publisher',2,TRUE,'{"scope":"WORKSPACE"}' WHERE NOT EXISTS(SELECT 1 FROM publishers WHERE code='KAFKA');
INSERT INTO publishers(code,label,description,sort_order,is_active,settings)
SELECT 'APP_CONFIG','AppConfig','Application publisher',3,TRUE,'{"scope":"APPLICATION"}' WHERE NOT EXISTS(SELECT 1 FROM publishers WHERE code='APP_CONFIG');
INSERT INTO schema_versions(identifier,schema_id,schema_version,version_name,definition,status,created_at)
SELECT UUID(),d.id,(SELECT COALESCE(MAX(v.schema_version),0)+1 FROM schema_versions v WHERE v.schema_id=d.id),'workspace-environment-configuration catalog v87','{"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object","additionalProperties":false,"required":["settings","publishers"],"properties":{"version":{"type":["integer","null"],"minimum":0,"maximum":9223372036854775807},"settings":{"type":"object","additionalProperties":true},"publishers":{"type":"array","minItems":1,"items":{"type":"object","additionalProperties":false,"required":["publisherCode","sortOrder","parameters"],"properties":{"sortOrder":{"type":"integer","minimum":1,"maximum":2147483647},"parameters":{"type":"object","additionalProperties":true},"publisherCode":{"type":"string","enum":["WEB_SOCKET","KAFKA","APP_CONFIG"]}}}},"environmentIdentifier":{"type":"string","format":"uuid"}},"if":{"required":["version"],"properties":{"version":{"type":"integer"}}},"then":{"not":{"required":["environmentIdentifier"]}},"else":{"required":["environmentIdentifier"]}}','PUBLISHED',NOW()
FROM schema_definitions d WHERE d.scope_code='PLATFORM' AND d.code='workspace-environment-configuration';
INSERT INTO schema_versions(identifier,schema_id,schema_version,version_name,definition,status,created_at)
SELECT UUID(),d.id,(SELECT COALESCE(MAX(v.schema_version),0)+1 FROM schema_versions v WHERE v.schema_id=d.id),'application-environment-configuration catalog v87','{"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object","additionalProperties":false,"required":["settings"],"properties":{"version":{"type":["integer","null"],"minimum":0,"maximum":9223372036854775807},"settings":{"type":"object","additionalProperties":true},"publishers":{"type":"array","minItems":1,"items":{"type":"object","additionalProperties":false,"required":["publisherCode","sortOrder","parameters"],"properties":{"sortOrder":{"type":"integer","minimum":1,"maximum":2147483647},"parameters":{"type":"object","additionalProperties":true},"publisherCode":{"type":"string","enum":["WEB_SOCKET","KAFKA","APP_CONFIG"]}}}},"environmentIdentifier":{"type":"string","format":"uuid"},"copyWorkspacePublishers":{"type":"boolean","default":false}},"allOf":[{"if":{"required":["version"],"properties":{"version":{"type":"integer"}}},"then":{"not":{"required":["environmentIdentifier"]}},"else":{"required":["environmentIdentifier"]}},{"if":{"required":["copyWorkspacePublishers"],"properties":{"copyWorkspacePublishers":{"const":true}}},"then":{"not":{"required":["publishers"]}},"else":{"required":["publishers"]}}]}','PUBLISHED',NOW()
FROM schema_definitions d WHERE d.scope_code='PLATFORM' AND d.code='application-environment-configuration';
INSERT INTO schema_definitions(version,identifier,scope_code,code,name,lifecycle_code,created_at,updated_at)
VALUES (0,UUID(),'PLATFORM','publisher-type-settings','Publisher catalog settings','ACTIVE',NOW(),NOW());
INSERT INTO schema_versions(identifier,schema_id,schema_version,version_name,definition,status,created_at)
SELECT UUID(),id,1,'Publisher catalog settings v1','{"type":"object","additionalProperties":true}','PUBLISHED',NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='publisher-type-settings';
INSERT INTO schema_binding(version,identifier,resource_type,resource_code,schema_id,lifecycle_code,created_at,updated_at)
SELECT 0,UUID(),'CATALOG','publisher-type',id,'ACTIVE',NOW(),NOW() FROM schema_definitions WHERE scope_code='PLATFORM' AND code='publisher-type-settings';

