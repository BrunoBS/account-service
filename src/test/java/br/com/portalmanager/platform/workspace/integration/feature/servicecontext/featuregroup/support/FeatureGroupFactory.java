package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support;

import br.com.portalmanager.platform.library.testing.fixture.factory.AbstractTestDataFactory;
import br.com.portalmanager.platform.workspace.entrypoint.servicecontext.featuregroup.request.CreateFeatureGroupRequest;
import br.com.portalmanager.platform.workspace.entrypoint.servicecontext.featuregroup.request.UpdateFeatureGroupRequest;

public final class FeatureGroupFactory extends AbstractTestDataFactory<CreateFeatureGroupRequest, FeatureGroupBuilder> {
    @Override protected FeatureGroupBuilder builder() { return FeatureGroupBuilder.builder(); }
    public CreateFeatureGroupRequest valid() { return builder().build(); }
    public CreateFeatureGroupRequest withCode(String value) { return builder().withCode(value).build(); }
    public CreateFeatureGroupRequest withoutCode() { return builder().withCode(null).build(); }
    public CreateFeatureGroupRequest withName(String value) { return builder().withName(value).build(); }
    public CreateFeatureGroupRequest withoutName() { return builder().withName(null).build(); }
    public CreateFeatureGroupRequest withInvalidCode() { return builder().withCode("Invalid code").build(); }
    public CreateFeatureGroupRequest withVersion() { return builder().withVersion(0L).build(); }
    public UpdateFeatureGroupRequest update(Long version, String code, String name) {
        return FeatureGroupUpdateBuilder.builder().withVersion(version).withCode(code).withName(name).build();
    }
    public UpdateFeatureGroupRequest updateWithoutVersion(String code, String name) { return update(null, code, name); }
    public UpdateFeatureGroupRequest withChangedCode(Long version) { return update(version, "changed-group", "Changed group"); }
}
