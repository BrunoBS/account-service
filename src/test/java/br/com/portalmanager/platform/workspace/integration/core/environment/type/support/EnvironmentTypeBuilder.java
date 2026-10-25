package br.com.portalmanager.platform.workspace.integration.core.environment.type.support;

import br.com.portalmanager.platform.library.testing.fixture.builder.TestDataBuilder;
import br.com.portalmanager.platform.workspace.entrypoint.environment.request.CreateEnvironmentTypeRequest;

public final class EnvironmentTypeBuilder implements TestDataBuilder<CreateEnvironmentTypeRequest> {
    private String code="CELL",name="Cell",description="Terminal environment type"; private Boolean rootAllowed=false,allowsChildren=false,workspaceRequired=true; private Integer displayOrder=20; private Long version;
    public static EnvironmentTypeBuilder builder(){return new EnvironmentTypeBuilder();}
    public EnvironmentTypeBuilder withCode(String v){code=v;return this;} public EnvironmentTypeBuilder withName(String v){name=v;return this;}
    public EnvironmentTypeBuilder withDescription(String v){description=v;return this;} public EnvironmentTypeBuilder withRootAllowed(Boolean v){rootAllowed=v;return this;}
    public EnvironmentTypeBuilder withAllowsChildren(Boolean v){allowsChildren=v;return this;} public EnvironmentTypeBuilder withWorkspaceRequired(Boolean v){workspaceRequired=v;return this;}
    public EnvironmentTypeBuilder withDisplayOrder(Integer v){displayOrder=v;return this;} public EnvironmentTypeBuilder withVersion(Long v){version=v;return this;}
    public CreateEnvironmentTypeRequest build(){return new CreateEnvironmentTypeRequest(version,code,name,description,rootAllowed,allowsChildren,workspaceRequired,displayOrder);}
}
