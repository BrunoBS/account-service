package br.com.portalmanager.platform.workspace.core.environment.integration;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.port.EnvironmentReferencePort;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.core.environment.domain.environment.Environment;
import br.com.portalmanager.platform.workspace.core.environment.repository.environment.EnvironmentRepository;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentFinder;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class EnvironmentReferenceAdapter
        implements EnvironmentReferencePort,
                br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.port
                        .EnvironmentReferencePort {
    private final EnvironmentRepository repository;
    private final EnvironmentFinder finder;

    public EnvironmentReferenceAdapter(EnvironmentRepository repository, EnvironmentFinder finder) {
        this.repository = repository;
        this.finder = finder;
    }

    @Override
    @Transactional
    public Long resolveActiveForUpdate(Long workspaceId, String identifier) {
        Environment reference =
                repository
                        .findByIdentifier(identifier)
                        .orElseThrow(() -> new NotFoundException(EnvironmentMessageKeys.NOT_FOUND));
        boolean globalDefault =
                reference.getWorkspaceId() == null
                        && "DEFAULT".equals(reference.getEnvironmentType().getCode());
        if (!globalDefault && !workspaceId.equals(reference.getWorkspaceId())) {
            throw new NotFoundException(EnvironmentMessageKeys.NOT_FOUND);
        }
        return globalDefault
                ? finder.findActiveForUpdate(identifier, null).getId()
                : finder.findWorkspaceActiveForUpdate(identifier, workspaceId).getId();
    }

    @Override
    @Transactional(readOnly = true)
    public String resolveIdentifier(Long id) {
        // The owning configuration has already passed workspace/application visibility.
        // Resolve only its persisted reference, including inactive environments, as in Shared
        // history.
        return resolveIdentifiers(List.of(id)).get(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> resolveIdentifiers(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        Map<Long, String> identifiers = repository.findIdentifierReferences(ids).stream()
                .collect(Collectors.toMap(EnvironmentRepository.IdentifierReference::getId,
                        EnvironmentRepository.IdentifierReference::getIdentifier));
        if (!identifiers.keySet().containsAll(new HashSet<>(ids))) {
            throw new NotFoundException(EnvironmentMessageKeys.NOT_FOUND);
        }
        return identifiers;
    }
}
