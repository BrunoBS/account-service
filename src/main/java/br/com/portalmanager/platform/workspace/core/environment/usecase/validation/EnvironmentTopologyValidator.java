package br.com.portalmanager.platform.workspace.core.environment.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.core.environment.domain.environment.Environment;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentType;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentTypeCompatibilityRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentTopologyValidator {

    private final EnvironmentTypeCompatibilityRepository compatibilities;

    public EnvironmentTopologyValidator(EnvironmentTypeCompatibilityRepository compatibilities) {
        this.compatibilities = compatibilities;
    }

    public void validate(EnvironmentType type, Long workspaceId, Environment parent) {
        ValidationResult result = new ValidationResult();
        if (type.isWorkspaceRequired() != (workspaceId != null)) result.addError(
            "environmentTypeCode",
            EnvironmentMessageKeys.TYPE_SCOPE_INVALID
        );
        if (parent == null) {
            if (!type.isRootAllowed()) result.addError("parentIdentifier", EnvironmentMessageKeys.ROOT_INVALID);
            reject(result);
            return;
        }
        if (parent.getWorkspaceId() != null && !Objects.equals(parent.getWorkspaceId(), workspaceId)) result.addError(
            "parentIdentifier",
            EnvironmentMessageKeys.PARENT_INVALID
        );
        if (
            !compatibilities.existsByParentTypeIdAndChildTypeIdAndLifecycle(
                parent.getEnvironmentType().getId(),
                type.getId(),
                LifecycleTypeCode.active()
            )
        ) result.addError("environmentTypeCode", EnvironmentMessageKeys.COMPATIBILITY_INVALID);
        reject(result);
    }

    public void requireParentAccessible(boolean accessible) {
        if (accessible) return;
        ValidationResult result = new ValidationResult();
        result.addError("parentIdentifier", EnvironmentMessageKeys.PARENT_INVALID);
        reject(result);
    }

    public void requireTypeChangeWithoutChildren(boolean hasChildren) {
        if (!hasChildren) return;
        ValidationResult result = new ValidationResult();
        result.addError("environmentTypeCode", EnvironmentMessageKeys.TYPE_IN_USE);
        reject(result);
    }

    private void reject(ValidationResult result) {
        if (result.hasErrors()) throw new ValidationException(result);
    }
}
