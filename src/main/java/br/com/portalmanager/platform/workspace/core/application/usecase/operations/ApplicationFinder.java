package br.com.portalmanager.platform.workspace.core.application.usecase.operations;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationMessageKeys;
import br.com.portalmanager.platform.workspace.core.application.repository.ApplicationRepository;
import org.springframework.stereotype.Component;

@Component
public class ApplicationFinder {
    private final ApplicationRepository repository;

    public ApplicationFinder(ApplicationRepository repository) {
        this.repository = repository;
    }

    public Application findByIdentifier(String identifier, Long workspaceId) {
        return repository.findByIdentifierAndWorkspaceId(identifier, workspaceId)
                .orElseThrow(() -> new NotFoundException(ApplicationMessageKeys.NOT_FOUND));
    }
}
