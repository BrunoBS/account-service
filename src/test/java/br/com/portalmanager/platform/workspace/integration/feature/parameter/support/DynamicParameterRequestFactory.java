package br.com.portalmanager.platform.workspace.integration.feature.parameter.support;

import br.com.portalmanager.platform.library.testing.fixture.factory.AbstractTestDataFactory;
import br.com.portalmanager.platform.workspace.entrypoint.parameter.request.CreateParameterKeyRequest;
import br.com.portalmanager.platform.workspace.entrypoint.parameter.request.UpdateParameterKeyRequest;
import tools.jackson.databind.json.JsonMapper;

/** Builds typed dynamic-parameter payloads and rejects malformed JSON fixtures. */
public final class DynamicParameterRequestFactory extends AbstractTestDataFactory<CreateParameterKeyRequest, DynamicParameterRequestBuilder> {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    @Override
    protected DynamicParameterRequestBuilder builder() {
        return new DynamicParameterRequestBuilder();
    }

    public CreateParameterKeyRequest valid(String parent, String key, String type, String valueJson) {
        return create(builder -> builder.parent(parent).key(key).type(type).valueJson(valueJson));
    }

    public CreateParameterKeyRequest withoutKey(String parent) {
        return create(builder -> builder.parent(parent).key(null));
    }

    public UpdateParameterKeyRequest update(long version, String description, String type, String valueJson) {
        try {
            return new UpdateParameterKeyRequest(version, description, type, JSON.readTree(valueJson));
        } catch (tools.jackson.core.JacksonException failure) {
            throw new IllegalArgumentException("Dynamic parameter fixture contains invalid JSON", failure);
        }
    }
}
