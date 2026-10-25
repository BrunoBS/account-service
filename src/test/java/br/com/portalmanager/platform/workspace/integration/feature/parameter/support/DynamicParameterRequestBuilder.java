package br.com.portalmanager.platform.workspace.integration.feature.parameter.support;

import br.com.portalmanager.platform.library.testing.fixture.builder.TestDataBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

final class DynamicParameterRequestBuilder implements TestDataBuilder<Map<String, Object>> {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private String parentIdentifier;
    private String key;
    private String type;
    private String valueJson;

    DynamicParameterRequestBuilder parent(String value) {
        parentIdentifier = value;
        return this;
    }

    DynamicParameterRequestBuilder key(String value) {
        key = value;
        return this;
    }

    DynamicParameterRequestBuilder type(String value) {
        type = value;
        return this;
    }

    DynamicParameterRequestBuilder valueJson(String value) {
        valueJson = value;
        return this;
    }

    @Override
    public Map<String, Object> build() {
        try {
            JsonNode value = MAPPER.readTree(valueJson);
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("microserviceIdentifier", parentIdentifier);
            request.put("key", key);
            request.put("type", type);
            request.put("value", value);
            return request;
        } catch (JacksonException failure) {
            throw new IllegalArgumentException("Dynamic parameter fixture contains invalid JSON", failure);
        }
    }
}
