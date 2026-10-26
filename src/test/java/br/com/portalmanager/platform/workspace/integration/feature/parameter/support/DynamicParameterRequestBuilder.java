package br.com.portalmanager.platform.workspace.integration.feature.parameter.support;

import br.com.portalmanager.platform.library.testing.fixture.builder.TestDataBuilder;
import br.com.portalmanager.platform.workspace.entrypoint.parameter.request.CreateParameterKeyRequest;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.util.UUID;

final class DynamicParameterRequestBuilder implements TestDataBuilder<CreateParameterKeyRequest> {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private UUID microserviceIdentifier;
    private String key;
    private String description = "Dynamic parameter integration fixture";
    private String type = "CONFIGURATION";
    private String valueJson = "{\"enabled\":true}";

    DynamicParameterRequestBuilder parent(String value) { microserviceIdentifier = UUID.fromString(value); return this; }
    DynamicParameterRequestBuilder key(String value) { key = value; return this; }
    DynamicParameterRequestBuilder description(String value) { description = value; return this; }
    DynamicParameterRequestBuilder type(String value) { type = value; return this; }
    DynamicParameterRequestBuilder valueJson(String value) { valueJson = value; return this; }

    @Override public CreateParameterKeyRequest build() {
        try {
            JsonNode value = JSON.readTree(valueJson);
            return new CreateParameterKeyRequest(null, microserviceIdentifier, key, description, type, value);
        } catch (JacksonException failure) {
            throw new IllegalArgumentException("Dynamic parameter fixture contains invalid JSON", failure);
        }
    }
}
