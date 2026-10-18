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
