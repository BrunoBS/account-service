package br.com.portalmanager.platform.workspace.core.environment.usecase.validation.compatibility;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentType;
import br.com.portalmanager.platform.workspace.core.environment.domain.compatibility.EnvironmentTypeCompatibility;
import br.com.portalmanager.platform.workspace.core.environment.repository.environment.EnvironmentRepository;
import br.com.portalmanager.platform.workspace.core.environment.repository.compatibility.EnvironmentTypeCompatibilityRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentTypeCompatibilityValidator {

    private final EnvironmentTypeCompatibilityRepository compatibilities;
    private final EnvironmentRepository environments;

    public EnvironmentTypeCompatibilityValidator(
        EnvironmentTypeCompatibilityRepository compatibilities,
        EnvironmentRepository environments
    ) {
        this.compatibilities = compatibilities;
        this.environments = environments;
    }

    public TypePair validateForCreate(String parentTypeCode, String childTypeCode, List<EnvironmentType> lockedTypes) {
        ValidationResult result = new ValidationResult();
        EnvironmentType parentType = findActiveType(parentTypeCode, lockedTypes);
        EnvironmentType childType = findActiveType(childTypeCode, lockedTypes);
        if (parentType == null) result.addError("parentTypeCode", EnvironmentMessageKeys.COMPATIBILITY_PARENT_INVALID);
        if (childType == null) result.addError("childTypeCode", EnvironmentMessageKeys.COMPATIBILITY_CHILD_INVALID);
        reject(result);

        if (!parentType.allowsChildren()) {
            result.addError("parentTypeCode", EnvironmentMessageKeys.PARENT_TYPE_CANNOT_HAVE_CHILDREN);
        }
        if (parentType.isWorkspaceRequired() && !childType.isWorkspaceRequired()) {
            result.addError("childTypeCode", EnvironmentMessageKeys.COMPATIBILITY_SCOPE_INVALID);
        }
        reject(result);

        if (parentType.getCode().equals(childType.getCode()) || createsCycle(parentType.getCode(), childType.getCode())) {
            result.addError("childTypeCode", EnvironmentMessageKeys.COMPATIBILITY_CYCLE_INVALID);
        }
        reject(result);
        return new TypePair(parentType, childType);
    }

    public void validateForDelete(EnvironmentTypeCompatibility compatibility) {
        ValidationResult result = new ValidationResult();
        if (!LifecycleTypeCode.inactive().equals(compatibility.getLifecycle())) {
            result.addError("lifecycle", EnvironmentMessageKeys.COMPATIBILITY_DELETE_REQUIRES_INACTIVE);
        }
        if (environments.findUsageForCompatibilityValidation(compatibility.getParentType().getCode(), compatibility.getChildType().getCode()).isPresent()) {
            result.addError("identifier", EnvironmentMessageKeys.COMPATIBILITY_IN_USE);
        }
        reject(result);
    }

    private boolean createsCycle(String parentTypeCode, String childTypeCode) {
        Map<String, Set<String>> childrenByTypeCode = new HashMap<>();
        for (EnvironmentTypeCompatibilityRepository.TypeLink link : compatibilities.findActiveTypeLinksForValidation()) {
            childrenByTypeCode.computeIfAbsent(link.getParentTypeCode(), ignored -> new HashSet<>()).add(link.getChildTypeCode());
        }
        ArrayDeque<String> pendingTypeCodes = new ArrayDeque<>();
        Set<String> visitedTypeCodes = new HashSet<>();
        pendingTypeCodes.add(childTypeCode);
        while (!pendingTypeCodes.isEmpty()) {
            String currentTypeCode = pendingTypeCodes.removeFirst();
            if (currentTypeCode.equals(parentTypeCode)) return true;
            if (visitedTypeCodes.add(currentTypeCode)) pendingTypeCodes.addAll(childrenByTypeCode.getOrDefault(currentTypeCode, Set.of()));
        }
        return false;
    }

    private EnvironmentType findActiveType(String code, List<EnvironmentType> lockedTypes) {
        if (code == null || code.isBlank()) return null;
        return lockedTypes.stream()
            .filter(type -> type.getCode().equalsIgnoreCase(code.trim()))
            .filter(type -> LifecycleTypeCode.active().equals(type.getLifecycle()))
            .findFirst().orElse(null);
    }

    private void reject(ValidationResult result) {
        if (result.hasErrors()) throw new ValidationException(result);
    }

    public record TypePair(EnvironmentType parentType, EnvironmentType childType) {}
}
