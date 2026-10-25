package br.com.portalmanager.platform.workspace.integration.foundation.schema.support;

import br.com.portalmanager.platform.library.testing.fixture.builder.TestDataBuilder;
import br.com.portalmanager.platform.workspace.entrypoint.schema.request.CreateSchemaBindingRequest;

public final class SchemaBindingBuilder implements TestDataBuilder<CreateSchemaBindingRequest> {
    private String resourceType = "INTEGRATION_TEST";
    private String resourceCode = "resource";
    private String schemaIdentifier;

    public static SchemaBindingBuilder builder() { return new SchemaBindingBuilder(); }
    public SchemaBindingBuilder withResourceType(String value) { resourceType = value; return this; }
    public SchemaBindingBuilder withResourceCode(String value) { resourceCode = value; return this; }
    public SchemaBindingBuilder withSchemaIdentifier(String value) { schemaIdentifier = value; return this; }

    @Override
    public CreateSchemaBindingRequest build() {
        return new CreateSchemaBindingRequest(resourceType, resourceCode, schemaIdentifier);
    }
}
