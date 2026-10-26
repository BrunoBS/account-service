package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support;

import br.com.portalmanager.platform.library.testing.fixture.factory.AbstractTestDataFactory;
import br.com.portalmanager.platform.workspace.entrypoint.servicecontext.microservice.request.CreateMicroserviceRequest;
import br.com.portalmanager.platform.workspace.entrypoint.servicecontext.microservice.request.UpdateMicroserviceRequest;
import tools.jackson.databind.json.JsonMapper;

public final class MicroserviceFactory extends AbstractTestDataFactory<CreateMicroserviceRequest, MicroserviceBuilder> {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    @Override protected MicroserviceBuilder builder() { return MicroserviceBuilder.builder(); }
    public CreateMicroserviceRequest valid() { return builder().build(); }
    public CreateMicroserviceRequest valid(String code, String name) {
        return builder().withCode(code).withName(name).build();
    }
    public CreateMicroserviceRequest withName(String value) { return builder().withName(value).build(); }
    public CreateMicroserviceRequest withCode(String value) { return builder().withCode(value).build(); }
    public CreateMicroserviceRequest withoutName() { return builder().withName(null).build(); }
    public CreateMicroserviceRequest withoutCode() { return builder().withCode(null).build(); }
    public CreateMicroserviceRequest withInvalidCode() { return builder().withCode("Invalid code").build(); }
    public CreateMicroserviceRequest withVersion() { return builder().withVersion(0L).build(); }
    public CreateMicroserviceRequest withInvalidSettings() {
        return builder().withSettings(JSON.valueToTree("settings must be an object")).build();
    }
    public UpdateMicroserviceRequest update(Long version, String code, String name) {
        return MicroserviceUpdateBuilder.builder().withVersion(version).withCode(code).withName(name)
                .withSettings(JSON.createObjectNode()).build();
    }
    public UpdateMicroserviceRequest updateWithoutVersion(String code, String name) {
        return update(null, code, name);
    }
    public UpdateMicroserviceRequest updateWithName(Long version, String code, String name) {
        return update(version, code, name);
    }
    public UpdateMicroserviceRequest updateWithChangedCode(Long version) {
        return update(version, "changed-code", "Changed code");
    }
}
