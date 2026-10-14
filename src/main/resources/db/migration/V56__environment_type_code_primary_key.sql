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
