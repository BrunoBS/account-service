package br.com.portalmanager.platform.workspace.core.environment.usecase.mapping;

import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentTypeDetails;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentTypeInput;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentTypeMapper {

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
