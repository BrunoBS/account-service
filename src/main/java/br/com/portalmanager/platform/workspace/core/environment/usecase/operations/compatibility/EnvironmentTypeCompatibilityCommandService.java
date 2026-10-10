package br.com.portalmanager.platform.workspace.core.environment.usecase.operations.compatibility;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentType;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentTypeCompatibility;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentRepository;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentTypeCompatibilityRepository;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentCompatibilityOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.validation.EnvironmentTypeCompatibilityValidator;
import br.com.portalmanager.platform.workspace.core.environment.usecase.validation.EnvironmentTypeCompatibilityValidator.TypePair;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnvironmentTypeCompatibilityCommandService {

    private final EnvironmentTypeCompatibilityRepository compatibilities;
    private final EnvironmentRepository environments;
    private final EnvironmentTypeCompatibilityValidator validator;

    public EnvironmentTypeCompatibilityCommandService(
        EnvironmentTypeCompatibilityRepository compatibilities,
        EnvironmentRepository environments,
        EnvironmentTypeCompatibilityValidator validator
    ) {
        this.compatibilities = compatibilities;
        this.environments = environments;
        this.validator = validator;
    }

    @Transactional
    public EnvironmentCompatibilityOutput allow(String parentCode, String childCode) {
        TypePair pair = validator.resolve(parentCode, childCode);
        EnvironmentType parent = pair.parent();
        EnvironmentType child = pair.child();
        boolean sameType = parent.getId().equals(child.getId());
        boolean cycle = !sameType && reaches(child.getId(), parent.getId(), new HashSet<>());
        validator.requireAcyclic(sameType, cycle);
        Optional<EnvironmentTypeCompatibility> existing = compatibilities.findByParentTypeIdAndChildTypeId(
            parent.getId(),
            child.getId()
        );
        if (existing.isPresent()) {
            existing.get().activate(LocalDateTime.now());
            return EnvironmentCompatibilityOutput.from(existing.get());
        }
        return EnvironmentCompatibilityOutput.from(
            compatibilities.saveAndFlush(new EnvironmentTypeCompatibility(parent, child, LocalDateTime.now()))
        );
    }

    private boolean reaches(Long current, Long target, Set<Long> visited) {
        if (current.equals(target)) return true;
        if (!visited.add(current)) return false;
        return compatibilities
            .findByLifecycle(LifecycleTypeCode.active())
            .stream()
            .filter(edge -> edge.getParentType().getId().equals(current))
            .anyMatch(edge -> reaches(edge.getChildType().getId(), target, visited));
    }

    @Transactional
    public void disallow(String identifier) {
        EnvironmentTypeCompatibility edge = find(identifier);
        edge.inactivate(LocalDateTime.now());
    }

    @Transactional
    public void delete(String identifier) {
        EnvironmentTypeCompatibility edge = find(identifier);
        validator.requireInactive(edge.getLifecycle());
        validator.requireUnused(
            environments.existsByTypePair(edge.getParentType().getId(), edge.getChildType().getId())
        );
        compatibilities.delete(edge);
        compatibilities.flush();
    }

    private EnvironmentTypeCompatibility find(String identifier) {
        return compatibilities
            .findByIdentifier(identifier)
            .orElseThrow(() -> new NotFoundException(EnvironmentMessageKeys.COMPATIBILITY_NOT_FOUND));
    }
}
