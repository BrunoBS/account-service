package br.com.portalmanager.platform.workspace.integration.foundation.schema.support;

import br.com.portalmanager.platform.library.testing.fixture.builder.TestDataBuilder;
import br.com.portalmanager.platform.workspace.entrypoint.schema.request.CreateSchemaVersionRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class SchemaVersionBuilder implements TestDataBuilder<CreateSchemaVersionRequest> {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private String versionName = "Draft version";
    private JsonNode definition = JSON.createObjectNode().put("type", "object").put("additionalProperties", true);

    public static SchemaVersionBuilder builder() { return new SchemaVersionBuilder(); }
    public SchemaVersionBuilder withVersionName(String value) { versionName = value; return this; }
    public SchemaVersionBuilder withDefinition(JsonNode value) { definition = value; return this; }

    @Override
    public CreateSchemaVersionRequest build() {
        return new CreateSchemaVersionRequest(versionName, definition);
    }
}
