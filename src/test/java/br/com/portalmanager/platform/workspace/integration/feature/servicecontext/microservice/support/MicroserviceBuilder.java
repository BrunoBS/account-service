package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support;

import br.com.portalmanager.platform.library.testing.fixture.builder.TestDataBuilder;
import br.com.portalmanager.platform.workspace.entrypoint.servicecontext.microservice.request.CreateMicroserviceRequest;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class MicroserviceBuilder implements TestDataBuilder<CreateMicroserviceRequest> {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private Long version;
    private String code = "service-" + UUID.randomUUID().toString().substring(0, 8);
    private String name = "Service " + UUID.randomUUID();
    private String description = "Service Context integration test";
    private JsonNode settings = JSON.createObjectNode();

    public static MicroserviceBuilder builder() { return new MicroserviceBuilder(); }
    public MicroserviceBuilder withVersion(Long value) { version = value; return this; }
    public MicroserviceBuilder withCode(String value) { code = value; return this; }
    public MicroserviceBuilder withName(String value) { name = value; return this; }
    public MicroserviceBuilder withDescription(String value) { description = value; return this; }
    public MicroserviceBuilder withSettings(JsonNode value) { settings = value; return this; }

    @Override public CreateMicroserviceRequest build() {
        return new CreateMicroserviceRequest(version, code, name, description, settings);
    }
}
