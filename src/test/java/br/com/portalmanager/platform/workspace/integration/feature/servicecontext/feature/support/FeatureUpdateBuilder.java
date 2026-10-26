package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support;

import br.com.portalmanager.platform.library.testing.fixture.builder.TestDataBuilder;
import br.com.portalmanager.platform.workspace.entrypoint.servicecontext.feature.request.UpdateFeatureRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class FeatureUpdateBuilder implements TestDataBuilder<UpdateFeatureRequest> {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private Long version;
    private String code;
    private String name;
    private String description = "Updated by integration test";
    private String microserviceIdentifier;
    private JsonNode settings = JSON.createObjectNode();
    private boolean shareable;
    public static FeatureUpdateBuilder builder() { return new FeatureUpdateBuilder(); }
    public FeatureUpdateBuilder withVersion(Long value) { version = value; return this; }
    public FeatureUpdateBuilder withCode(String value) { code = value; return this; }
    public FeatureUpdateBuilder withName(String value) { name = value; return this; }
    public FeatureUpdateBuilder withDescription(String value) { description = value; return this; }
    public FeatureUpdateBuilder withMicroservice(String value) { microserviceIdentifier = value; return this; }
    public FeatureUpdateBuilder withSettings(JsonNode value) { settings = value; return this; }
    public FeatureUpdateBuilder withShareable(boolean value) { shareable = value; return this; }
    @Override public UpdateFeatureRequest build() { return new UpdateFeatureRequest(version, code, name, description, microserviceIdentifier, settings, shareable); }
}
