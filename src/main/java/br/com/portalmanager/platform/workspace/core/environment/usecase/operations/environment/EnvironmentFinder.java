package br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.environment.domain.Environment;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentFinder {

    private final EnvironmentRepository repository;

    public EnvironmentFinder(EnvironmentRepository repository) {
        this.repository = repository;
    }

    public Environment findActive(String identifier, Long workspaceId) {
        Environment e = find(identifier, workspaceId);
        if (!accessible(e)) throw new NotFoundException(EnvironmentMessageKeys.NOT_FOUND);
        return e;
    }

    public boolean accessible(Environment environment) {
        for (Environment current = environment; current != null; current = current.getParent()) {
            if (!LifecycleTypeCode.active().equals(current.getLifecycle())) return false;
        }
        return true;
    }

    public Environment findInactive(String identifier, Long workspaceId) {
        Environment e = find(identifier, workspaceId);
        if (!LifecycleTypeCode.inactive().equals(e.getLifecycle())) throw new ValidationException(
            EnvironmentMessageKeys.RESTORE_INVALID
        );
        return e;
    }

    public Environment findInactiveForDeletion(String identifier, Long workspaceId) {
        Environment e = find(identifier, workspaceId);
        if (!LifecycleTypeCode.inactive().equals(e.getLifecycle())) throw new ValidationException(
            EnvironmentMessageKeys.DELETE_INVALID
        );
        return e;
    }

    private Environment find(String identifier, Long workspaceId) {
        Optional<Environment> value =
            workspaceId == null
                ? repository.findByIdentifierAndWorkspaceIdIsNull(identifier)
                : repository.findByIdentifierAndWorkspaceId(identifier, workspaceId);
        return value.orElseThrow(() -> new NotFoundException(EnvironmentMessageKeys.NOT_FOUND));
    }
}
