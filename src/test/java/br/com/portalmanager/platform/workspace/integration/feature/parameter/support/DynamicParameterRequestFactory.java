package br.com.portalmanager.platform.workspace.integration.feature.parameter.support;

import br.com.portalmanager.platform.library.testing.fixture.factory.AbstractTestDataFactory;

import java.util.Map;

/** Builds typed dynamic-parameter payloads and rejects malformed JSON fixtures. */
final class DynamicParameterRequestFactory extends AbstractTestDataFactory<Map<String, Object>, DynamicParameterRequestBuilder> {
    @Override
    protected DynamicParameterRequestBuilder builder() {
        return new DynamicParameterRequestBuilder();
    }

    Map<String, Object> valid(String parent, String key, String type, String valueJson) {
        return create(builder -> builder.parent(parent).key(key).type(type).valueJson(valueJson));
    }
}
