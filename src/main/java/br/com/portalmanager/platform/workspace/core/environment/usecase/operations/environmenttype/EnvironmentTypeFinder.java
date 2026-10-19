package br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environmenttype;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentType;
import br.com.portalmanager.platform.workspace.core.environment.repository.environmenttype.EnvironmentTypeRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EnvironmentTypeFinder {

    private final EnvironmentTypeRepository repository;
    private final EntityManager entityManager;

    public EnvironmentTypeFinder(EnvironmentTypeRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public EnvironmentType findForEnvironmentCreation(
        String typeCode,
        boolean workspaceEnvironment,
        String parentTypeCode
    ) {
        EnvironmentType environmentType = findTypeAndParentWithReadLock(typeCode, parentTypeCode, true);
        if (workspaceEnvironment != environmentType.isWorkspaceRequired()) {
            throw notFound();
        }
        return environmentType;
    }

    public EnvironmentType findForEnvironmentRestoration(String typeCode, String parentTypeCode) {
        return findTypeAndParentWithReadLock(typeCode, parentTypeCode, false);
    }

    public EnvironmentType findForEnvironmentUpdate(
        String currentTypeCode,
        String requestedTypeCode,
        String parentTypeCode
    ) {
        requireCode(requestedTypeCode);
        boolean changingType = !requestedTypeCode.equals(currentTypeCode);
        return findTypeAndParentWithReadLock(requestedTypeCode, parentTypeCode, changingType);
    }

    @Transactional(readOnly = true)
    public EnvironmentType find(String typeCode) {
        requireCode(typeCode);
        return repository.findById(typeCode).orElseThrow(this::notFound);
    }

    public EnvironmentType findForUpdate(String typeCode) {
        requireCode(typeCode);
        detachCachedType(typeCode);
        return repository.findByCodeForUpdate(typeCode).orElseThrow(this::notFound);
    }

    public EnvironmentType findForConfigurationUpdate(String typeCode) {
        requireCode(typeCode);
        return selectType(typeCode, findAllForUpdate());
    }

    public List<EnvironmentType> findAllForUpdate() {
        repository.findAllCodes().forEach(this::detachCachedType);
        return repository.findAllForUpdate();
    }

    private EnvironmentType findTypeAndParentWithReadLock(
        String typeCode,
        String parentTypeCode,
        boolean requireActiveTypes
    ) {
        requireCode(typeCode);
        if (parentTypeCode != null) requireCode(parentTypeCode);

        List<String> orderedTypeCodes = parentTypeCode == null || parentTypeCode.equals(typeCode)
            ? List.of(typeCode)
            : Stream.of(typeCode, parentTypeCode).sorted().toList();

        List<EnvironmentType> lockedTypes = new ArrayList<>(orderedTypeCodes.size());
        for (String lockedTypeCode : orderedTypeCodes) {
            EnvironmentType environmentType = findWithReadLock(lockedTypeCode);
            if (requireActiveTypes && !LifecycleTypeCode.active().equals(environmentType.getLifecycle())) {
                throw notFound();
            }
            lockedTypes.add(environmentType);
        }
        return selectType(typeCode, lockedTypes);
    }

    private EnvironmentType findWithReadLock(String typeCode) {
        detachCachedType(typeCode);
        return repository.findByCodeForRead(typeCode).orElseThrow(this::notFound);
    }

    private void detachCachedType(String typeCode) {
        // Hibernate obtains this reference without a SELECT; detaching forces the locking query to reload it.
        EnvironmentType cachedType = entityManager.getReference(EnvironmentType.class, typeCode);
        entityManager.detach(cachedType);
    }

    private EnvironmentType selectType(String typeCode, List<EnvironmentType> environmentTypes) {
        return environmentTypes.stream()
            .filter(environmentType -> typeCode.equals(environmentType.getCode()))
            .findFirst()
            .orElseThrow(this::notFound);
    }

    private void requireCode(String typeCode) {
        if (typeCode == null || typeCode.isBlank()) throw notFound();
    }

    private NotFoundException notFound() {
        return new NotFoundException(EnvironmentMessageKeys.TYPE_NOT_FOUND);
    }
}
