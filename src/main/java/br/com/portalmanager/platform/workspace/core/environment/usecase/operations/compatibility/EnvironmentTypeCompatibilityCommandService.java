package br.com.portalmanager.platform.workspace.core.environment.usecase.operations.compatibility;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentType;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentTypeCompatibility;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentRepository;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentTypeCompatibilityRepository;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentCompatibilityOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environmenttype.EnvironmentTypeQueryService;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
public class EnvironmentTypeCompatibilityCommandService {
    private final EnvironmentTypeCompatibilityRepository compatibilities;
    private final EnvironmentRepository environments;
    private final EnvironmentTypeQueryService types;

    public EnvironmentTypeCompatibilityCommandService(EnvironmentTypeCompatibilityRepository compatibilities,
                                                      EnvironmentRepository environments, EnvironmentTypeQueryService types) {
        this.compatibilities = compatibilities;
        this.environments = environments;
        this.types = types;
    }

    @Transactional
    public EnvironmentCompatibilityOutput allow(String parentCode, String childCode) {
        EnvironmentType parent = types.active(parentCode);
        EnvironmentType child = types.active(childCode);
        if (parent.getId().equals(child.getId()) || reaches(child.getId(), parent.getId(), new HashSet<>()))
            throw new ValidationException(EnvironmentMessageKeys.COMPATIBILITY_INVALID);
        var existing = compatibilities.findByParentTypeIdAndChildTypeId(parent.getId(), child.getId());
        if (existing.isPresent()) {
            existing.get().activate(LocalDateTime.now());
            return EnvironmentCompatibilityOutput.from(existing.get());
        }
        return EnvironmentCompatibilityOutput.from(compatibilities.saveAndFlush(
                new EnvironmentTypeCompatibility(parent, child, LocalDateTime.now())));
    }

    private boolean reaches(Long current, Long target, Set<Long> visited) {
        if (current.equals(target)) return true;
        if (!visited.add(current)) return false;
        return compatibilities.findByLifecycle(LifecycleTypeCode.active()).stream()
                .filter(edge -> edge.getParentType().getId().equals(current))
                .anyMatch(edge -> reaches(edge.getChildType().getId(), target, visited));
    }

    @Transactional
    public void disallow(String identifier) {
        EnvironmentTypeCompatibility edge = compatibilities.findByIdentifier(identifier)
                .orElseThrow(() -> new NotFoundException(EnvironmentMessageKeys.COMPATIBILITY_NOT_FOUND));
        if (environments.existsByTypePair(edge.getParentType().getId(), edge.getChildType().getId()))
            throw new ValidationException(EnvironmentMessageKeys.TYPE_IN_USE);
        edge.inactivate(LocalDateTime.now());
    }
}
