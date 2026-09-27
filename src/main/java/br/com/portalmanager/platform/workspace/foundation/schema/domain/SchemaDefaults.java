package br.com.portalmanager.platform.workspace.foundation.schema.domain;

public final class SchemaDefaults {

    public static final String DEFAULT_SCHEMA_TYPE_CODE = "DEFAULT";
    /**
     * Definition seeded by V17 as the published DEFAULT / PLATFORM schema.
     * Runtime resolution reads the published version from the database.
     */
    public static final String DEFAULT_JSON_SCHEMA = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "title": "Default Dynamic Schema",
              "type": "object",
              "additionalProperties": true
            }
            """;

    private SchemaDefaults() {
    }
}
