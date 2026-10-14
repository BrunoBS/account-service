package br.com.portalmanager.platform.workspace.core.environment.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.core.environment.domain.environment.Environment;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentType;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentRepository;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentTypeCompatibilityRepository;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentTypeRepository;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentTopologyValidator {

    private final EnvironmentTypeCompatibilityRepository compatibilities;
    private final EnvironmentTypeRepository types;
    private final EnvironmentRepository environments;

    public EnvironmentTopologyValidator(
        EnvironmentTypeCompatibilityRepository compatibilities,
        EnvironmentTypeRepository types,
        EnvironmentRepository environments
    ) {
        this.compatibilities = compatibilities;
        this.types = types;
        this.environments = environments;
    }

    public void validateForNewLink(EnvironmentType type, Long workspaceId, Environment parent) {
        validate(type, workspaceId, parent, true);
    }

    public void validateForExistingLink(EnvironmentType type, Long workspaceId, Environment parent) {
        validate(type, workspaceId, parent, false);
    }

    public void validateForUpdate(Environment environment, EnvironmentType requestedType) {
        boolean typeChanged = !requestedType.getCode().equals(environment.getEnvironmentType().getCode());
        if (!typeChanged) {
            validateForExistingLink(requestedType, environment.getWorkspaceId(), environment.getParent());
            return;
        }

        validateForNewLink(requestedType, environment.getWorkspaceId(), environment.getParent());
        if (environments.findChildForEnvironmentValidation(environment.getId()).isPresent()) {
            ValidationResult result = new ValidationResult();
            result.addError("environmentTypeCode", EnvironmentMessageKeys.TYPE_IN_USE);
            reject(result);
        }
    }

    public void validateParentTypeUnchanged(String expectedTypeCode, String currentTypeCode) {
        if (!Objects.equals(expectedTypeCode, currentTypeCode)) {
            throw new ValidationException(EnvironmentMessageKeys.PARENT_INVALID);
        }
    }

    private void validate(EnvironmentType type, Long workspaceId, Environment parent, boolean requireActiveCompatibility) {
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
        EnvironmentType parentType = types
            .findByCodeForRead(parent.getEnvironmentType().getCode())
            .orElseThrow(() -> new NotFoundException(EnvironmentMessageKeys.TYPE_NOT_FOUND));
        if (!parentType.allowsChildren()) result.addError(
            "parentIdentifier",
            EnvironmentMessageKeys.PARENT_TYPE_CANNOT_HAVE_CHILDREN
        );
        if (
            requireActiveCompatibility && !compatibilities.findActiveLinkForValidation(parentType.getCode(), type.getCode()).isPresent()
        ) result.addError("environmentTypeCode", EnvironmentMessageKeys.COMPATIBILITY_INVALID);
        reject(result);
    }

    private void reject(ValidationResult result) {
        if (result.hasErrors()) throw new ValidationException(result);
    }
}
