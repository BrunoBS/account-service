package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support;

import br.com.portalmanager.platform.library.testing.fixture.factory.AbstractTestDataFactory;
import br.com.portalmanager.platform.workspace.entrypoint.servicecontext.feature.request.CreateFeatureRequest;
import br.com.portalmanager.platform.workspace.entrypoint.servicecontext.feature.request.UpdateFeatureRequest;
import tools.jackson.databind.json.JsonMapper;

public final class FeatureFactory extends AbstractTestDataFactory<CreateFeatureRequest, FeatureBuilder> {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    @Override protected FeatureBuilder builder() { return FeatureBuilder.builder(); }
    public CreateFeatureRequest valid(String microserviceIdentifier) { return builder().withMicroservice(microserviceIdentifier).build(); }
    public CreateFeatureRequest withName(String microserviceIdentifier, String name) { return builder().withMicroservice(microserviceIdentifier).withName(name).build(); }
    public CreateFeatureRequest withCode(String microserviceIdentifier, String code) { return builder().withMicroservice(microserviceIdentifier).withCode(code).build(); }
    public CreateFeatureRequest withoutCode(String microserviceIdentifier) { return builder().withMicroservice(microserviceIdentifier).withCode(null).build(); }
    public CreateFeatureRequest withoutName(String microserviceIdentifier) { return builder().withMicroservice(microserviceIdentifier).withName(null).build(); }
    public CreateFeatureRequest withoutSettings(String microserviceIdentifier) { return builder().withMicroservice(microserviceIdentifier).withSettings(null).build(); }
    public CreateFeatureRequest withInvalidSettings(String microserviceIdentifier) { return builder().withMicroservice(microserviceIdentifier).withSettings(JSON.valueToTree("invalid")).build(); }
    public CreateFeatureRequest withInvalidCode(String microserviceIdentifier) { return builder().withMicroservice(microserviceIdentifier).withCode("Invalid code").build(); }
    public CreateFeatureRequest withMicroservice(String microserviceIdentifier) { return builder().withMicroservice(microserviceIdentifier).build(); }
    public CreateFeatureRequest withoutMicroservice() { return builder().build(); }
    public CreateFeatureRequest withVersion(String microserviceIdentifier) { return builder().withMicroservice(microserviceIdentifier).withVersion(0L).build(); }
    public CreateFeatureRequest shareable(String microserviceIdentifier) { return builder().withMicroservice(microserviceIdentifier).withShareable(true).build(); }
    public UpdateFeatureRequest update(Long version, String code, String name, String microserviceIdentifier, boolean shareable) {
        return FeatureUpdateBuilder.builder().withVersion(version).withCode(code).withName(name)
                .withMicroservice(microserviceIdentifier).withSettings(JSON.createObjectNode())
                .withShareable(shareable).build();
    }
    public UpdateFeatureRequest updateWithoutVersion(String code, String name, String microserviceIdentifier) { return update(null, code, name, microserviceIdentifier, false); }
    public UpdateFeatureRequest updateWithChangedCode(Long version, String microserviceIdentifier) { return update(version, "changed-feature", "Changed feature", microserviceIdentifier, false); }
}
