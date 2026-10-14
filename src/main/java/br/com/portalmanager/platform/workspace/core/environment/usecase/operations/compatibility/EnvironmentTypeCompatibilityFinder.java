package br.com.portalmanager.platform.workspace.core.environment.usecase.operations.compatibility;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentTypeCompatibility;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentTypeCompatibilityRepository;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EnvironmentTypeCompatibilityFinder {

    private final EnvironmentTypeCompatibilityRepository repository;
    private final EntityManager entityManager;

    public EnvironmentTypeCompatibilityFinder(
        EnvironmentTypeCompatibilityRepository repository,
        EntityManager entityManager
    ) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public EnvironmentTypeCompatibility findForUpdate(String identifier) {
        EnvironmentTypeCompatibility compatibility = repository.findByIdentifierForUpdate(identifier)
            .orElseThrow(() -> new NotFoundException(EnvironmentMessageKeys.COMPATIBILITY_NOT_FOUND));
        entityManager.detach(compatibility);
        return repository.findByIdentifierForUpdate(identifier)
            .orElseThrow(() -> new NotFoundException(EnvironmentMessageKeys.COMPATIBILITY_NOT_FOUND));
    }

    public Optional<EnvironmentTypeCompatibility> findByTypePairForUpdate(String parentTypeCode, String childTypeCode) {
        Optional<EnvironmentTypeCompatibility> existing = repository.findByTypePairForUpdate(parentTypeCode, childTypeCode);
        existing.ifPresent(entityManager::detach);
        return existing.isEmpty() ? existing : repository.findByTypePairForUpdate(parentTypeCode, childTypeCode);
    }
}
