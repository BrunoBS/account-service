ALTER TABLE schema_types
ADD COLUMN scope_code VARCHAR(50) NULL;

UPDATE schema_types st
JOIN schema_type_scopes sts ON sts.schema_type_id = st.id
SET
    st.scope_code = sts.scope_code;

ALTER TABLE schema_types
MODIFY COLUMN scope_code VARCHAR(50) NOT NULL,
ADD CONSTRAINT fk_schema_types_scope FOREIGN KEY (scope_code) REFERENCES type_schema_scopes (code);

DROP TABLE schema_type_scopes;
