package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support;

import br.com.portalmanager.platform.library.testing.fixture.builder.TestDataBuilder;
import br.com.portalmanager.platform.workspace.entrypoint.servicecontext.featuregroup.request.UpdateFeatureGroupRequest;

public final class FeatureGroupUpdateBuilder implements TestDataBuilder<UpdateFeatureGroupRequest> {
    private Long version;
    private String code;
    private String name;
    private String description = "Updated by integration test";
    public static FeatureGroupUpdateBuilder builder() { return new FeatureGroupUpdateBuilder(); }
    public FeatureGroupUpdateBuilder withVersion(Long value) { version = value; return this; }
    public FeatureGroupUpdateBuilder withCode(String value) { code = value; return this; }
    public FeatureGroupUpdateBuilder withName(String value) { name = value; return this; }
    public FeatureGroupUpdateBuilder withDescription(String value) { description = value; return this; }
    @Override public UpdateFeatureGroupRequest build() { return new UpdateFeatureGroupRequest(version, code, name, description); }
}
