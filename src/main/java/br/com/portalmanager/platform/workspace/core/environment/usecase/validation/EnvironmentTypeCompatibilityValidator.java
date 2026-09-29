package br.com.portalmanager.platform.workspace.core.environment.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentType;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentTypeRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentTypeCompatibilityValidator {
    private final EnvironmentTypeRepository types;

    public EnvironmentTypeCompatibilityValidator(EnvironmentTypeRepository types) { this.types = types; }

    public TypePair resolve(String parentCode, String childCode) {
        ValidationResult result = new ValidationResult();
        EnvironmentType parent = active(parentCode);
        EnvironmentType child = active(childCode);
        if (parent == null) result.addError("parentTypeCode", EnvironmentMessageKeys.COMPATIBILITY_PARENT_INVALID);
        if (child == null) result.addError("childTypeCode", EnvironmentMessageKeys.COMPATIBILITY_CHILD_INVALID);
        reject(result);
        return new TypePair(parent, child);
    }

    public void requireAcyclic(boolean selfRelation, boolean cycle) {
        ValidationResult result = new ValidationResult();
        if (selfRelation || cycle)
            result.addError("childTypeCode", EnvironmentMessageKeys.COMPATIBILITY_CYCLE_INVALID);
        reject(result);
    }

    public void requireInactive(LifecycleTypeCode lifecycle) {
        ValidationResult result = new ValidationResult();
        if (!LifecycleTypeCode.inactive().equals(lifecycle))
            result.addError("identifier", EnvironmentMessageKeys.COMPATIBILITY_DELETE_REQUIRES_INACTIVE);
        reject(result);
    }

    public void requireUnused(boolean inUse) {
        ValidationResult result = new ValidationResult();
        if (inUse) result.addError("identifier", EnvironmentMessageKeys.COMPATIBILITY_IN_USE);
        reject(result);
    }

    private EnvironmentType active(String code) {
        if (code == null || code.isBlank()) return null;
        return types.findByCodeAndLifecycle(code, LifecycleTypeCode.active()).orElse(null);
    }

    private void reject(ValidationResult result) {
        if (result.hasErrors()) throw new ValidationException(result);
    }

    public record TypePair(EnvironmentType parent, EnvironmentType child) {}
}
