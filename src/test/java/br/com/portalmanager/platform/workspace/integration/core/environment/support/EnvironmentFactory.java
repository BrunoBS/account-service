package br.com.portalmanager.platform.workspace.integration.core.environment.support;

import br.com.portalmanager.platform.library.testing.fixture.factory.AbstractTestDataFactory;
import br.com.portalmanager.platform.workspace.entrypoint.environment.request.CreateEnvironmentRequest;
import br.com.portalmanager.platform.workspace.entrypoint.environment.request.UpdateEnvironmentRequest;
import tools.jackson.databind.JsonNode;

public final class EnvironmentFactory extends AbstractTestDataFactory<CreateEnvironmentRequest,EnvironmentBuilder> {
    @Override protected EnvironmentBuilder builder(){return EnvironmentBuilder.builder();}
    public CreateEnvironmentRequest valid(){return builder().build();}
    public CreateEnvironmentRequest validDefault(){return builder().withEnvironmentType("DEFAULT").build();}
    public CreateEnvironmentRequest validDefault(String name){return builder().withEnvironmentType("DEFAULT").withName(name).build();}
    public CreateEnvironmentRequest valid(String name){return builder().withName(name).build();}
    public CreateEnvironmentRequest custom(String name,String parent){return builder().withName(name).withParentIdentifier(parent).build();}
    public CreateEnvironmentRequest shard(String name,String parent){return builder().withName(name).withEnvironmentType("SHARD").withParentIdentifier(parent).build();}
    public CreateEnvironmentRequest withInvalidType(){return builder().withEnvironmentType("INVALID").build();}
    public CreateEnvironmentRequest withInvalidSettings(){return builder().withSettings(null).build();}
    public CreateEnvironmentRequest withoutName(){return builder().withName(null).build();}
    public CreateEnvironmentRequest withoutDescription(){return builder().withDescription(null).build();}
    public CreateEnvironmentRequest withAuthorizationType(String value){return builder().withAuthorizationType(value).build();}
    public CreateEnvironmentRequest withName(String value){return builder().withName(value).build();}
    public UpdateEnvironmentRequest update(Long version, JsonNode settings) {
        return new UpdateEnvironmentRequest(version, "Renamed environment", "Updated environment description",
                "TST", null, null, settings, "CUSTOM", null);
    }
    public UpdateEnvironmentRequest updateWithoutName(Long version, JsonNode settings) {
        return new UpdateEnvironmentRequest(version, null, "Updated environment description",
                "TST", null, null, settings, "CUSTOM", null);
    }
    public UpdateEnvironmentRequest updateWithoutVersion(JsonNode settings) {
        return update(null, settings);
    }
    public UpdateEnvironmentRequest updateWithoutEnvironmentType(Long version, JsonNode settings) {
        return new UpdateEnvironmentRequest(version, "Renamed environment", "Updated environment description",
                "TST", null, null, settings, null, null);
    }
    public UpdateEnvironmentRequest updateWithParent(Long version, JsonNode settings, String parentIdentifier) {
        return new UpdateEnvironmentRequest(version, "Renamed environment", "Updated environment description",
                "TST", null, null, settings, "CUSTOM", parentIdentifier);
    }
}
