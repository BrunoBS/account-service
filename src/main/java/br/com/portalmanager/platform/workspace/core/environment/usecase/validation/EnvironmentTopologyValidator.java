package br.com.portalmanager.platform.workspace.core.environment.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.environment.domain.Environment;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentType;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentTypeCompatibilityRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import org.springframework.stereotype.Component;
import java.util.Objects;

@Component
public class EnvironmentTopologyValidator {
    private final EnvironmentTypeCompatibilityRepository compatibilities;

    public EnvironmentTopologyValidator(EnvironmentTypeCompatibilityRepository compatibilities) {
        this.compatibilities = compatibilities;
    }

    public void validate(EnvironmentType type, Long workspaceId, Environment parent) {
        if (type.isWorkspaceRequired() != (workspaceId != null))
            throw new ValidationException(EnvironmentMessageKeys.TYPE_SCOPE_INVALID);
        if (parent == null) {
            if (!type.isRootAllowed()) throw new ValidationException(EnvironmentMessageKeys.ROOT_INVALID);
            return;
        }
        if (parent.getWorkspaceId() != null && !Objects.equals(parent.getWorkspaceId(), workspaceId))
            throw new ValidationException(EnvironmentMessageKeys.PARENT_INVALID);
        if (!compatibilities.existsByParentTypeIdAndChildTypeIdAndLifecycle(
                parent.getEnvironmentType().getId(), type.getId(), LifecycleTypeCode.active()))
            throw new ValidationException(EnvironmentMessageKeys.COMPATIBILITY_INVALID);
    }
}
