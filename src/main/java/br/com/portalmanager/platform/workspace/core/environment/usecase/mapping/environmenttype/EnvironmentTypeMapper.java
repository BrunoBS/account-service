package br.com.portalmanager.platform.workspace.core.environment.usecase.mapping.environmenttype;

import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentTypeDetails;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentType;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environmenttype.EnvironmentTypeInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environmenttype.EnvironmentTypeOutput;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentTypeMapper {

    public EnvironmentTypeOutput toOutput(EnvironmentType type) {
        return new EnvironmentTypeOutput(type.getVersion(), type.getCode(), type.getCode(), type.getName(),
            type.getDescription(), type.isRootAllowed(), type.allowsChildren(), type.isWorkspaceRequired(),
            type.getDisplayOrder(), type.getLifecycle().value());
    }

    public EnvironmentTypeDetails toDetails(EnvironmentTypeInput input) {
        return new EnvironmentTypeDetails(
            input.name(),
            input.description(),
            input.rootAllowed(),
            input.allowsChildren(),
            input.workspaceRequired(),
            input.displayOrder()
        );
    }
}
