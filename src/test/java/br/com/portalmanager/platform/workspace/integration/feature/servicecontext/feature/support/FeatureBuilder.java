package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support;

import br.com.portalmanager.platform.library.testing.fixture.builder.TestDataBuilder;
import br.com.portalmanager.platform.workspace.entrypoint.servicecontext.feature.request.CreateFeatureRequest;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class FeatureBuilder implements TestDataBuilder<CreateFeatureRequest> {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private Long version;
    private String code = "feature-" + UUID.randomUUID().toString().substring(0, 8);
    private String name = "Feature " + UUID.randomUUID();
    private String description = "Service Context integration test";
    private String microserviceIdentifier;
    private JsonNode settings = JSON.createObjectNode();
    private boolean shareable;
    public static FeatureBuilder builder() { return new FeatureBuilder(); }
    public FeatureBuilder withCode(String value) { code = value; return this; }
    public FeatureBuilder withVersion(Long value) { version = value; return this; }
    public FeatureBuilder withName(String value) { name = value; return this; }
    public FeatureBuilder withDescription(String value) { description = value; return this; }
    public FeatureBuilder withMicroservice(String value) { microserviceIdentifier = value; return this; }
    public FeatureBuilder withSettings(JsonNode value) { settings = value; return this; }
    public FeatureBuilder withShareable(boolean value) { shareable = value; return this; }
    @Override public CreateFeatureRequest build() {
        return new CreateFeatureRequest(version, code, name, description, microserviceIdentifier, settings, shareable);
    }
}
