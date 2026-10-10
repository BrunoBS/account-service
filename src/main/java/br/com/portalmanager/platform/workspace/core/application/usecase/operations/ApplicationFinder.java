package br.com.portalmanager.platform.workspace.core.application.usecase.operations;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationMessageKeys;
import br.com.portalmanager.platform.workspace.core.application.repository.ApplicationRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import org.springframework.stereotype.Component;

@Component
public class ApplicationFinder {

    private final ApplicationRepository repository;

    public ApplicationFinder(ApplicationRepository repository) {
        this.repository = repository;
    }

    public Application findActive(String identifier, Long workspaceId) {
        Application app = repository
            .findByIdentifierAndWorkspaceId(identifier, workspaceId)
            .orElseThrow(() -> new NotFoundException(ApplicationMessageKeys.NOT_FOUND));
        if (!LifecycleTypeCode.active().equals(app.getLifecycle())) throw new NotFoundException(
            ApplicationMessageKeys.NOT_FOUND
        );
        return app;
    }

    public Application findInactive(String identifier, Long workspaceId) {
        Application app = repository
            .findByIdentifierAndWorkspaceId(identifier, workspaceId)
            .orElseThrow(() -> new NotFoundException(ApplicationMessageKeys.NOT_FOUND));
        if (!LifecycleTypeCode.inactive().equals(app.getLifecycle())) throw new ValidationException(
            ApplicationMessageKeys.RESTORE_INVALID
        );
        return app;
    }

    public Application findInactiveForDeletion(String identifier, Long workspaceId) {
        Application app = repository
            .findByIdentifierAndWorkspaceId(identifier, workspaceId)
            .orElseThrow(() -> new NotFoundException(ApplicationMessageKeys.NOT_FOUND));
        if (!LifecycleTypeCode.inactive().equals(app.getLifecycle())) throw new ValidationException(
            ApplicationMessageKeys.DELETE_INVALID
        );
        return app;
    }
}
