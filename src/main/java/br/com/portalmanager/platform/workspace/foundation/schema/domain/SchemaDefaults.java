package br.com.portalmanager.platform.workspace.foundation.schema.domain;

public final class SchemaDefaults {

    public static final String DEFAULT_SCHEMA_TYPE_CODE = "DEFAULT";
    /**
     * Restrictive fallback used when validating generic values before a specific
     * platform schema is resolved.
     */
    public static final String DEFAULT_JSON_SCHEMA = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "title": "Default Dynamic Schema",
              "type": "object",
              "additionalProperties": {
                "type": ["string", "number", "boolean", "null"]
              }
            }
            """;
    /**
     * Schema V2 platform fallback. This is the contract seeded by V17 and used
     * only through SchemaResolver when no specific PLATFORM schema exists.
     */
    public static final String PLATFORM_DEFAULT_JSON_SCHEMA = """
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
