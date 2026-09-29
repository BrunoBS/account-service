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
