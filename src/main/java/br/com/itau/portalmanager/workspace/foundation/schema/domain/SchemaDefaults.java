package br.com.itau.portalmanager.workspace.foundation.schema.domain;

public final class SchemaDefaults {

    private SchemaDefaults() {
    }

    public static final String DEFAULT_JSON_SCHEMA = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "title": "Default Dynamic Schema",
              "type": "object",
              "additionalProperties": true
            }
            """;
}
