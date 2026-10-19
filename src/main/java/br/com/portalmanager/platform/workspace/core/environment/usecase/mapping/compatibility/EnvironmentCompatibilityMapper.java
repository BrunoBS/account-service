package br.com.portalmanager.platform.workspace.core.environment.usecase.mapping.compatibility;

import br.com.portalmanager.platform.workspace.core.environment.domain.compatibility.EnvironmentTypeCompatibility;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.compatibility.EnvironmentCompatibilityOutput;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentCompatibilityMapper {

    public EnvironmentCompatibilityOutput toOutput(EnvironmentTypeCompatibility compatibility) {
        return new EnvironmentCompatibilityOutput(
            compatibility.getIdentifier(),
            compatibility.getParentType().getCode(),
            compatibility.getChildType().getCode(),
            compatibility.getLifecycle().value()
        );
    }
}
