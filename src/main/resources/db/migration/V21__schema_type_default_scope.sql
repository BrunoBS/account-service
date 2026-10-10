UPDATE schema_types
SET
    lifecycle_code = 'ACTIVE'
WHERE
    code = 'DEFAULT';

UPDATE schema_definitions
SET
    lifecycle_code = 'ACTIVE'
WHERE
    schema_type_code = 'DEFAULT'
    AND scope_code = 'PLATFORM';

DELETE sts
FROM
    schema_type_scopes sts
    JOIN schema_types st ON st.id = sts.schema_type_id
WHERE
    st.code = 'DEFAULT'
    AND sts.scope_code <> 'PLATFORM';

INSERT INTO
    schema_type_scopes (schema_type_id, scope_code)
SELECT
    st.id,
    'PLATFORM'
FROM
    schema_types st
WHERE
    st.code = 'DEFAULT'
    AND NOT EXISTS (
        SELECT
            1
        FROM
            schema_type_scopes sts
        WHERE
            sts.schema_type_id = st.id
            AND sts.scope_code = 'PLATFORM'
    );
