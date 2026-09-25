CREATE TABLE platform_feature_contexts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    identifier VARCHAR(36) NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    lifecycle_code VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_platform_feature_contexts PRIMARY KEY (id),
    CONSTRAINT uk_platform_feature_contexts_identifier UNIQUE (identifier),
    CONSTRAINT uk_platform_feature_contexts_code UNIQUE (code),
    CONSTRAINT uk_platform_feature_contexts_name UNIQUE (name),
    CONSTRAINT fk_platform_feature_contexts_lifecycle
        FOREIGN KEY (lifecycle_code) REFERENCES type_life_cycle(code)
);

INSERT INTO platform_feature_contexts
    (version, identifier, code, name, description, lifecycle_code, created_at, updated_at)
SELECT
    0,
    UUID(),
    code,
    UPPER(REPLACE(code, '-', '_')),
    description,
    CASE WHEN is_active THEN 'ACTIVE' ELSE 'INACTIVE' END,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM type_feature_scopes;

CREATE TABLE platform_feature_context_relations (
    feature_id BIGINT NOT NULL,
    feature_context_id BIGINT NOT NULL,
    CONSTRAINT pk_platform_feature_context_relations PRIMARY KEY (feature_id, feature_context_id),
    CONSTRAINT fk_platform_feature_context_relations_feature
        FOREIGN KEY (feature_id) REFERENCES platform_features(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_platform_feature_context_relations_context
        FOREIGN KEY (feature_context_id) REFERENCES platform_feature_contexts(id)
);

INSERT INTO platform_feature_context_relations (feature_id, feature_context_id)
SELECT
    relation.feature_id,
    context.id
FROM platform_feature_scopes relation
JOIN platform_feature_contexts context
  ON context.code = relation.feature_scope_code;

UPDATE platform_services
   SET name = UPPER(REPLACE(code, '-', '_'));

UPDATE platform_features
   SET name = UPPER(REPLACE(code, '-', '_'));

ALTER TABLE platform_services
    ADD CONSTRAINT uk_platform_services_name UNIQUE (name);

ALTER TABLE platform_features
    ADD CONSTRAINT uk_platform_features_name UNIQUE (name);

DROP TABLE platform_feature_scopes;
DROP TABLE type_feature_scopes;
