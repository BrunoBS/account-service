package br.com.portalmanager.platform.workspace.integration.core.environment.type.support;

import br.com.portalmanager.platform.library.testing.fixture.factory.AbstractTestDataFactory;
import br.com.portalmanager.platform.workspace.entrypoint.environment.request.CreateEnvironmentTypeRequest;
import br.com.portalmanager.platform.workspace.entrypoint.environment.request.UpdateEnvironmentTypeRequest;

public final class EnvironmentTypeFactory extends AbstractTestDataFactory<CreateEnvironmentTypeRequest,EnvironmentTypeBuilder> {
    @Override
    protected EnvironmentTypeBuilder builder() {
        return EnvironmentTypeBuilder.builder();
    }

    public CreateEnvironmentTypeRequest valid() {
        return builder().build();
    }

    public CreateEnvironmentTypeRequest withCode(String code) {
        return builder().withCode(code).build();
    }

    public CreateEnvironmentTypeRequest withScope(Boolean root, Boolean children, Boolean workspace) {
        return builder().withRootAllowed(root).withAllowsChildren(children).withWorkspaceRequired(workspace).build();
    }

    public CreateEnvironmentTypeRequest withoutName() {
        return builder().withName(null).build();
    }

    public UpdateEnvironmentTypeRequest updated(Long version) {
        return new UpdateEnvironmentTypeRequest(version, "CELL", "Cell updated", "Updated terminal type",
                false, false, true, 10);
    }

    public UpdateEnvironmentTypeRequest withInvalidScope(Long version) {
        return new UpdateEnvironmentTypeRequest(version, "CELL", "Cell", "Terminal type",
                true, false, false, 10);
    }

    public UpdateEnvironmentTypeRequest withChangedCode(Long version) {
        return new UpdateEnvironmentTypeRequest(version, "SHARD", "Cell", "Terminal type",
                false, false, true, 10);
    }
}
