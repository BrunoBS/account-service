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
