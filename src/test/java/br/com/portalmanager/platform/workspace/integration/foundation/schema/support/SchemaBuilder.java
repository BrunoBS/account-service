package br.com.portalmanager.platform.workspace.integration.foundation.schema.support;

import br.com.portalmanager.platform.library.testing.fixture.builder.TestDataBuilder;
import br.com.portalmanager.platform.workspace.entrypoint.schema.request.CreateSchemaRequest;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class SchemaBuilder implements TestDataBuilder<CreateSchemaRequest> {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private String code = "schema-" + UUID.randomUUID().toString().substring(0, 8);
    private String name = "Integration schema";
    private String description = "Schema fixture for HTTP integration tests";
    private String versionName = "Initial draft";
    private JsonNode definition = JSON.createObjectNode().put("type", "object").put("additionalProperties", true);

    public static SchemaBuilder builder() { return new SchemaBuilder(); }
    public SchemaBuilder withCode(String value) { code = value; return this; }
    public SchemaBuilder withName(String value) { name = value; return this; }
    public SchemaBuilder withDescription(String value) { description = value; return this; }
    public SchemaBuilder withVersionName(String value) { versionName = value; return this; }
    public SchemaBuilder withDefinition(JsonNode value) { definition = value; return this; }

    @Override
    public CreateSchemaRequest build() {
        return new CreateSchemaRequest(code, name, description, versionName, definition);
    }
}
