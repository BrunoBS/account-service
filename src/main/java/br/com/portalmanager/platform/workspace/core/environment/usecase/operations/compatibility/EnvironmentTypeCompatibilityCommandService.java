package br.com.portalmanager.platform.workspace.core.environment.usecase.operations.compatibility;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentType;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentTypeCompatibility;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentTypeCompatibilityRepository;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentCompatibilityOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environmenttype.EnvironmentTypeFinder;
import br.com.portalmanager.platform.workspace.core.environment.usecase.validation.EnvironmentTypeCompatibilityValidator.TypePair;
import br.com.portalmanager.platform.workspace.core.environment.usecase.validation.EnvironmentTypeCompatibilityValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnvironmentTypeCompatibilityCommandService {

    private final EnvironmentTypeCompatibilityRepository repository;
    private final EnvironmentTypeFinder typeFinder;
    private final EnvironmentTypeCompatibilityFinder finder;
    private final EnvironmentTypeCompatibilityValidator validator;

    public EnvironmentTypeCompatibilityCommandService(EnvironmentTypeCompatibilityRepository repository, EnvironmentTypeFinder typeFinder,
        EnvironmentTypeCompatibilityFinder finder, EnvironmentTypeCompatibilityValidator validator) {
        this.repository = repository;
        this.typeFinder = typeFinder;
        this.finder = finder;
        this.validator = validator;
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, event = "ENVIRONMENT_TYPE_COMPATIBILITY_ALLOWED", resourceType = "ENVIRONMENT_TYPE_COMPATIBILITY")
    public EnvironmentCompatibilityOutput create(String parentTypeCode, String childTypeCode) {
        List<EnvironmentType> lockedTypes = typeFinder.findAllForUpdate();
        TypePair typePair = validator.validateForCreate(parentTypeCode, childTypeCode, lockedTypes);
        Optional<EnvironmentTypeCompatibility> existingCompatibility = finder.findByTypePairForUpdate(typePair.parentType().getCode(), typePair.childType().getCode());

        if (existingCompatibility.isPresent()) {
            EnvironmentTypeCompatibility compatibility = existingCompatibility.get();
            if (!LifecycleTypeCode.active().equals(compatibility.getLifecycle())) {
                compatibility.restore(LocalDateTime.now());
            }
            return EnvironmentCompatibilityOutput.from(repository.saveAndFlush(compatibility));
        }
        EnvironmentTypeCompatibility compatibility = new EnvironmentTypeCompatibility(typePair.parentType(), typePair.childType(), LocalDateTime.now());
        return EnvironmentCompatibilityOutput.from(repository.saveAndFlush(compatibility));
    }

    @Transactional
    @Auditable(action = AuditAction.DEACTIVATE, event = "ENVIRONMENT_TYPE_COMPATIBILITY_DISALLOWED", resourceType = "ENVIRONMENT_TYPE_COMPATIBILITY")
    public EnvironmentCompatibilityOutput inactivate(String identifier) {
        typeFinder.findAllForUpdate();
        EnvironmentTypeCompatibility compatibility = finder.findForUpdate(identifier);
        compatibility.inactivate(LocalDateTime.now());
        return EnvironmentCompatibilityOutput.from(repository.saveAndFlush(compatibility));
    }

    @Transactional
    @Auditable(action = AuditAction.PURGE, event = "ENVIRONMENT_TYPE_COMPATIBILITY_PURGED", resourceType = "ENVIRONMENT_TYPE_COMPATIBILITY")
    public EnvironmentCompatibilityOutput delete(String identifier) {
        typeFinder.findAllForUpdate();
        EnvironmentTypeCompatibility compatibility = finder.findForUpdate(identifier);
        validator.validateForDelete(compatibility);
        EnvironmentCompatibilityOutput removedCompatibility = EnvironmentCompatibilityOutput.from(compatibility);
        repository.delete(compatibility);
        repository.flush();
        return removedCompatibility;
    }
}
