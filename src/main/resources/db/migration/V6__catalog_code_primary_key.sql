ALTER TABLE workspaces
DROP CHECK ck_workspaces_workspace_type;

ALTER TABLE type_workspaces
MODIFY COLUMN id BIGINT NOT NULL;

ALTER TABLE type_workspaces
DROP PRIMARY KEY;

ALTER TABLE type_workspaces
DROP COLUMN id,
RENAME COLUMN name TO code;

ALTER TABLE type_workspaces
ADD CONSTRAINT pk_type_workspaces PRIMARY KEY (code);

ALTER TABLE type_application_scopes
MODIFY COLUMN id BIGINT NOT NULL;

ALTER TABLE type_application_scopes
DROP PRIMARY KEY;

ALTER TABLE type_application_scopes
DROP COLUMN id,
RENAME COLUMN name TO code;

ALTER TABLE type_application_scopes
ADD CONSTRAINT pk_type_application_scopes PRIMARY KEY (code);

ALTER TABLE type_authorizations
MODIFY COLUMN id BIGINT NOT NULL;

ALTER TABLE type_authorizations
DROP PRIMARY KEY;

ALTER TABLE type_authorizations
DROP COLUMN id,
RENAME COLUMN name TO code;

ALTER TABLE type_authorizations
ADD CONSTRAINT pk_type_authorizations PRIMARY KEY (code);

ALTER TABLE type_feature_scopes
MODIFY COLUMN id BIGINT NOT NULL;

ALTER TABLE type_feature_scopes
DROP PRIMARY KEY;

ALTER TABLE type_feature_scopes
DROP COLUMN id,
RENAME COLUMN name TO code;

ALTER TABLE type_feature_scopes
ADD CONSTRAINT pk_type_feature_scopes PRIMARY KEY (code);

ALTER TABLE type_features
MODIFY COLUMN id BIGINT NOT NULL;

ALTER TABLE type_features
DROP PRIMARY KEY;

ALTER TABLE type_features
DROP COLUMN id,
RENAME COLUMN name TO code;

ALTER TABLE type_features
ADD CONSTRAINT pk_type_features PRIMARY KEY (code);

ALTER TABLE type_infrastructures
MODIFY COLUMN id BIGINT NOT NULL;

ALTER TABLE type_infrastructures
DROP PRIMARY KEY;

ALTER TABLE type_infrastructures
DROP COLUMN id,
RENAME COLUMN name TO code;

ALTER TABLE type_infrastructures
ADD CONSTRAINT pk_type_infrastructures PRIMARY KEY (code);

ALTER TABLE type_languages
MODIFY COLUMN id BIGINT NOT NULL;

ALTER TABLE type_languages
DROP PRIMARY KEY;

ALTER TABLE type_languages
DROP COLUMN id,
RENAME COLUMN name TO code;

ALTER TABLE type_languages
ADD CONSTRAINT pk_type_languages PRIMARY KEY (code);

ALTER TABLE type_life_cycle
MODIFY COLUMN id BIGINT NOT NULL;

ALTER TABLE type_life_cycle
DROP PRIMARY KEY;

ALTER TABLE type_life_cycle
DROP COLUMN id,
RENAME COLUMN name TO code;

ALTER TABLE type_life_cycle
ADD CONSTRAINT pk_type_life_cycle PRIMARY KEY (code);

ALTER TABLE type_tag_origins
MODIFY COLUMN id BIGINT NOT NULL;

ALTER TABLE type_tag_origins
DROP PRIMARY KEY;

ALTER TABLE type_tag_origins
DROP COLUMN id,
RENAME COLUMN name TO code;

ALTER TABLE type_tag_origins
ADD CONSTRAINT pk_type_tag_origins PRIMARY KEY (code);

ALTER TABLE type_visibilities
MODIFY COLUMN id BIGINT NOT NULL;

ALTER TABLE type_visibilities
DROP PRIMARY KEY;

ALTER TABLE type_visibilities
DROP COLUMN id,
RENAME COLUMN name TO code;

ALTER TABLE type_visibilities
ADD CONSTRAINT pk_type_visibilities PRIMARY KEY (code);

ALTER TABLE type_onboardings
MODIFY COLUMN id BIGINT NOT NULL;

ALTER TABLE type_onboardings
DROP PRIMARY KEY;

ALTER TABLE type_onboardings
DROP COLUMN id,
RENAME COLUMN name TO code;

ALTER TABLE type_onboardings
ADD CONSTRAINT pk_type_onboardings PRIMARY KEY (code);

ALTER TABLE type_publisher_scopes
MODIFY COLUMN id BIGINT NOT NULL;

ALTER TABLE type_publisher_scopes
DROP PRIMARY KEY;

ALTER TABLE type_publisher_scopes
DROP COLUMN id,
RENAME COLUMN name TO code;

ALTER TABLE type_publisher_scopes
ADD CONSTRAINT pk_type_publisher_scopes PRIMARY KEY (code);

ALTER TABLE type_schema_scopes
MODIFY COLUMN id BIGINT NOT NULL;

ALTER TABLE type_schema_scopes
DROP PRIMARY KEY;

ALTER TABLE type_schema_scopes
DROP COLUMN id,
RENAME COLUMN name TO code;

ALTER TABLE type_schema_scopes
ADD CONSTRAINT pk_type_schema_scopes PRIMARY KEY (code);

ALTER TABLE type_schemas
MODIFY COLUMN id BIGINT NOT NULL;

ALTER TABLE type_schemas
DROP PRIMARY KEY;

ALTER TABLE type_schemas
DROP COLUMN id,
RENAME COLUMN name TO code;

ALTER TABLE type_schemas
ADD CONSTRAINT pk_type_schemas PRIMARY KEY (code);

ALTER TABLE type_sharing_statuses
MODIFY COLUMN id BIGINT NOT NULL;

ALTER TABLE type_sharing_statuses
DROP PRIMARY KEY;

ALTER TABLE type_sharing_statuses
DROP COLUMN id,
RENAME COLUMN name TO code;

ALTER TABLE type_sharing_statuses
ADD CONSTRAINT pk_type_sharing_statuses PRIMARY KEY (code);

INSERT IGNORE INTO
    type_workspaces (
        code,
        label,
        description,
        sort_order,
        is_active,
        settings
    )
VALUES
    (
        'ADMIN',
        'Admin',
        'Administrative workspace',
        1,
        true,
        '{}'
    ),
    (
        'MANAGER',
        'Manager',
        'Management workspace',
        2,
        true,
        '{}'
    ),
    (
        'CATALOG',
        'Catalog',
        'Catalog workspace',
        3,
        true,
        '{}'
    );

ALTER TABLE workspaces
RENAME COLUMN workspace_type TO workspace_type_code;

ALTER TABLE workspaces
MODIFY COLUMN workspace_type_code VARCHAR(50) NOT NULL,
ADD CONSTRAINT fk_workspaces_workspace_type FOREIGN KEY (workspace_type_code) REFERENCES type_workspaces (code);
