package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support;

import br.com.portalmanager.platform.library.testing.fixture.builder.TestDataBuilder;
import br.com.portalmanager.platform.workspace.entrypoint.servicecontext.featuregroup.request.CreateFeatureGroupRequest;
import java.util.UUID;

public final class FeatureGroupBuilder implements TestDataBuilder<CreateFeatureGroupRequest> {
    private Long version;
    private String code = "group-" + UUID.randomUUID().toString().substring(0, 8);
    private String name = "Feature group " + UUID.randomUUID();
    private String description = "Service Context integration test";
    public static FeatureGroupBuilder builder() { return new FeatureGroupBuilder(); }
    public FeatureGroupBuilder withVersion(Long value) { version = value; return this; }
    public FeatureGroupBuilder withCode(String value) { code = value; return this; }
    public FeatureGroupBuilder withName(String value) { name = value; return this; }
    public FeatureGroupBuilder withDescription(String value) { description = value; return this; }
    @Override public CreateFeatureGroupRequest build() { return new CreateFeatureGroupRequest(version, code, name, description); }
}
