package br.com.portalmanager.platform.workspace.core.environment.usecase.validation.environmenttype;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentType;
import br.com.portalmanager.platform.workspace.core.environment.repository.environment.EnvironmentRepository;
import br.com.portalmanager.platform.workspace.core.environment.repository.compatibility.EnvironmentTypeCompatibilityRepository;
import br.com.portalmanager.platform.workspace.core.environment.repository.environmenttype.EnvironmentTypeRepository;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environmenttype.EnvironmentTypeInput;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentTypeValidator {

    private final EnvironmentTypeRepository types;

    private final EnvironmentRepository environments;

    private final EnvironmentTypeCompatibilityRepository compatibilities;

    public EnvironmentTypeValidator(
        EnvironmentTypeRepository types,
        EnvironmentRepository environments,
        EnvironmentTypeCompatibilityRepository compatibilities
    ) {
        this.types = types;
        this.environments = environments;
        this.compatibilities = compatibilities;
    }

    public void validateForCreate(EnvironmentTypeInput input) {
        ValidationResult result = new ValidationResult();
        if (input == null) {
            result.addError("request", EnvironmentMessageKeys.TYPE_REQUEST_INVALID);
        } else {
            if (input.version() != null) {
                result.addError("version", EnvironmentMessageKeys.TYPE_VERSION_NOT_ALLOWED_ON_CREATE);
            }
            if (types.existsByCode(input.code())) {
                result.addError("code", EnvironmentMessageKeys.TYPE_DUPLICATE);
            }
            validateScopeCombination(input, result);
        }
        reject(result);
    }

    public void validateForUpdate(EnvironmentType type, EnvironmentTypeInput input) {
        type.requireActiveForUpdate();
        ValidationResult result = new ValidationResult();
        if (input == null) result.addError("request", EnvironmentMessageKeys.TYPE_REQUEST_INVALID);
        else if (input.version() == null || input.version() < 0) result.addError(
            "version", EnvironmentMessageKeys.TYPE_VERSION_REQUIRED
        );
        else validateScopeCombination(input, result);
        reject(result);

        if (!Objects.equals(type.getVersion(), input.version())) throw new ResourceVersionConflictException();
        if (!Objects.equals(type.getCode(), input.code())) result.addError("code", EnvironmentMessageKeys.TYPE_CODE_IMMUTABLE);
        if (type.isWorkspaceRequired() != input.workspaceRequired() && environments.findUsageForTypeValidation(type.getCode()).isPresent()) {
            result.addError("workspaceRequired", EnvironmentMessageKeys.TYPE_IN_USE);
        }
        if (type.isRootAllowed() && !input.rootAllowed() && environments.findRootForTypeValidation(type.getCode()).isPresent()) {
            result.addError("rootAllowed", EnvironmentMessageKeys.TYPE_IN_USE);
        }
        if (!input.allowsChildren()) {
            if (compatibilities.findActiveChildLinkForTypeValidation(type.getCode()).isPresent()) {
                result.addError("allowsChildren", EnvironmentMessageKeys.TYPE_CHILD_COMPATIBILITIES_ACTIVE);
            }
            if (environments.findChildForTypeValidation(type.getCode()).isPresent()) {
                result.addError("allowsChildren", EnvironmentMessageKeys.TYPE_CHILDREN_IN_USE);
            }
        }
        reject(result);
    }

    public void validateForDelete(EnvironmentType type) {
        ValidationResult result = new ValidationResult();
        if (!LifecycleTypeCode.inactive().equals(type.getLifecycle())) {
            result.addError("lifecycle", EnvironmentMessageKeys.TYPE_DELETE_REQUIRES_INACTIVE);
        }
        if (environments.findUsageForTypeValidation(type.getCode()).isPresent() ||
            compatibilities.findUsageForTypeValidation(type.getCode()).isPresent()) {
            result.addError("code", EnvironmentMessageKeys.TYPE_IN_USE);
        }
        reject(result);
    }

    private void reject(ValidationResult result) {
        if (result.hasErrors()) throw new ValidationException(result);
    }

    private void validateScopeCombination(EnvironmentTypeInput input, ValidationResult result) {
        if (input.code() == null) {
            result.addError("code", EnvironmentMessageKeys.TYPE_CODE_INVALID);
            return;
        }
        boolean valid = switch (input.code()) {
            case "DEFAULT" -> input.rootAllowed() && !input.workspaceRequired();
            case "CUSTOM" -> input.rootAllowed() && input.workspaceRequired();
            case "SHARD", "CELL" -> !input.rootAllowed() && input.workspaceRequired();
            default -> {
                result.addError("code", EnvironmentMessageKeys.TYPE_CODE_INVALID);
                yield true;
            }
        };
        if (!valid) result.addError("workspaceRequired", EnvironmentMessageKeys.TYPE_SCOPE_INVALID);
    }
}
