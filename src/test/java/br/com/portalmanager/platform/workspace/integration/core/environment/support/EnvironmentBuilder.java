package br.com.portalmanager.platform.workspace.integration.core.environment.support;

import br.com.portalmanager.platform.library.testing.fixture.builder.TestDataBuilder;
import br.com.portalmanager.platform.workspace.entrypoint.environment.request.CreateEnvironmentRequest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

public final class EnvironmentBuilder implements TestDataBuilder<CreateEnvironmentRequest> {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private Long version;
    private String name = "Environment " + UUID.randomUUID();
    private String description = "Valid environment description";
    private String authorizationType = "DEV";
    private Integer sortOrder;
    private String authorizerGroup;
    private JsonNode settings = JSON.createObjectNode();
    private String environmentTypeCode = "CUSTOM";
    private String parentIdentifier;

    public static EnvironmentBuilder builder() {
        return new EnvironmentBuilder();
    }

    public EnvironmentBuilder withVersion(Long v) {
        version = v;
        return this;
    }

    public EnvironmentBuilder withName(String v) {
        name = v;
        return this;
    }

    public EnvironmentBuilder withDescription(String v) {
        description = v;
        return this;
    }

    public EnvironmentBuilder withAuthorizationType(String v) {
        authorizationType = v;
        return this;
    }

    public EnvironmentBuilder withSortOrder(Integer v) {
        sortOrder = v;
        return this;
    }

    public EnvironmentBuilder withAuthorizerGroup(String v) {
        authorizerGroup = v;
        return this;
    }

    public EnvironmentBuilder withSettings(JsonNode v) {
        settings = v;
        return this;
    }

    public EnvironmentBuilder withEnvironmentType(String v) {
        environmentTypeCode = v;
        return this;
    }

    public EnvironmentBuilder withParentIdentifier(String v) {
        parentIdentifier = v;
        return this;
    }

    public CreateEnvironmentRequest build() {
        return new CreateEnvironmentRequest(version, name, description, authorizationType, sortOrder, authorizerGroup, settings, environmentTypeCode, parentIdentifier);
    }
}
