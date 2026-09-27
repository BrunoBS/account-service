package br.com.portalmanager.platform.workspace.core.environment.usecase.operations;

import br.com.portalmanager.platform.library.authorization.annotation.ResourceVisibility;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentRepository;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentOutput;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceReferenceResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class EnvironmentQueryService {
    private final EnvironmentRepository repository;
    private final EnvironmentFinder finder;
    private final WorkspaceReferenceResolver workspaces;
    public EnvironmentQueryService(EnvironmentRepository repository, EnvironmentFinder finder, WorkspaceReferenceResolver workspaces) {
        this.repository = repository;
        this.finder = finder;
        this.workspaces = workspaces;
    }

    @ResourceVisibility
    @Transactional(readOnly = true)
    public EnvironmentOutput findCustom(String workspaceIdentifier, String identifier) {
        return EnvironmentOutput.from(finder.findActive(identifier, workspaces.resolveInternalId(workspaceIdentifier)), workspaceIdentifier);
    }

    @ResourceVisibility
    @Transactional(readOnly = true)
    public EnvironmentOutput findCustomInactive(String workspaceIdentifier, String identifier) {
        return EnvironmentOutput.from(finder.findInactive(identifier, workspaces.resolveInternalId(workspaceIdentifier)), workspaceIdentifier);
    }

    @ResourceVisibility
    @Transactional(readOnly = true)
    public EnvironmentOutput findCustomForDeletion(String workspaceIdentifier, String identifier) {
        return EnvironmentOutput.from(finder.findInactiveForDeletion(identifier, workspaces.resolveInternalId(workspaceIdentifier)), workspaceIdentifier);
    }

    @ResourceVisibility
    @Transactional(readOnly = true)
    public List<EnvironmentOutput> listCustom(String workspaceIdentifier, Boolean active) {
        Long id = workspaces.resolveInternalId(workspaceIdentifier);
        return repository.findByWorkspaceAndLifecycle(id, lifecycle(active)).stream()
                .map(e -> EnvironmentOutput.from(e, workspaceIdentifier)).toList();
    }

    @Transactional(readOnly = true)
    public EnvironmentOutput findDefault(String identifier) { return EnvironmentOutput.from(finder.findActive(identifier, null), null); }
    @Transactional(readOnly = true)
    public EnvironmentOutput findDefaultInactive(String identifier) { return EnvironmentOutput.from(finder.findInactive(identifier, null), null); }
    @Transactional(readOnly = true)
    public EnvironmentOutput findDefaultForDeletion(String identifier) { return EnvironmentOutput.from(finder.findInactiveForDeletion(identifier, null), null); }
    @Transactional(readOnly = true)
    public List<EnvironmentOutput> listDefaults(Boolean active) {
        return repository.findDefaultsByLifecycle(lifecycle(active)).stream().map(e -> EnvironmentOutput.from(e, null)).toList();
    }

    private String lifecycle(Boolean active) {
        return Boolean.FALSE.equals(active) ? LifecycleTypeCode.inactive().value() : LifecycleTypeCode.active().value();
    }
}
