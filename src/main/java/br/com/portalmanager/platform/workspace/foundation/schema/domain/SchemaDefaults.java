package br.com.portalmanager.platform.workspace.foundation.schema.domain;

public final class SchemaDefaults {

    /**
     * Built-in JSON Schema returned when no active, published platform schema applies.
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
