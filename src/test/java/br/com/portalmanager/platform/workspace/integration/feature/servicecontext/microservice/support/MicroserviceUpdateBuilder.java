package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support;

import br.com.portalmanager.platform.library.testing.fixture.builder.TestDataBuilder;
import br.com.portalmanager.platform.workspace.entrypoint.servicecontext.microservice.request.UpdateMicroserviceRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class MicroserviceUpdateBuilder implements TestDataBuilder<UpdateMicroserviceRequest> {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private Long version;
    private String code;
    private String name;
    private String description = "Updated by integration test";
    private JsonNode settings = JSON.createObjectNode();
    public static MicroserviceUpdateBuilder builder() { return new MicroserviceUpdateBuilder(); }
    public MicroserviceUpdateBuilder withVersion(Long value) { version = value; return this; }
    public MicroserviceUpdateBuilder withCode(String value) { code = value; return this; }
    public MicroserviceUpdateBuilder withName(String value) { name = value; return this; }
    public MicroserviceUpdateBuilder withDescription(String value) { description = value; return this; }
    public MicroserviceUpdateBuilder withSettings(JsonNode value) { settings = value; return this; }
    @Override public UpdateMicroserviceRequest build() { return new UpdateMicroserviceRequest(version, code, name, description, settings); }
}
