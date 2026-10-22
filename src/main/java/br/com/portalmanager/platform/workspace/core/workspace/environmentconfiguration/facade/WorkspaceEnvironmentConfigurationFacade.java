package br.com.portalmanager.platform.workspace.core.workspace.environmentconfiguration.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.*;
import br.com.portalmanager.platform.workspace.core.workspace.environmentconfiguration.usecase.model.*;
import br.com.portalmanager.platform.workspace.core.workspace.environmentconfiguration.usecase.operations.WorkspaceEnvironmentConfigurationCommandService;
import br.com.portalmanager.platform.workspace.core.workspace.environmentconfiguration.usecase.operations.WorkspaceEnvironmentConfigurationQueryService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceEnvironmentConfigurationFacade {
    private final WorkspaceEnvironmentConfigurationCommandService commands;
    private final WorkspaceEnvironmentConfigurationQueryService queries;

    public WorkspaceEnvironmentConfigurationFacade(
            WorkspaceEnvironmentConfigurationCommandService commands,
            WorkspaceEnvironmentConfigurationQueryService queries) {
        this.commands = commands;
        this.queries = queries;
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.CREATE)
    public WorkspaceEnvironmentConfigurationOutput create(
            AuthorizationContext context,
            String workspaceIdentifier,
            CreateWorkspaceEnvironmentConfigurationInput input) {
        return commands.create(workspaceIdentifier, input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.UPDATE)
    public WorkspaceEnvironmentConfigurationOutput update(
            AuthorizationContext context,
            String workspaceIdentifier,
            String identifier,
            UpdateWorkspaceEnvironmentConfigurationInput input) {
        return commands.update(workspaceIdentifier, identifier, input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public WorkspaceEnvironmentConfigurationOutput find(
            AuthorizationContext context, String workspaceIdentifier, String identifier) {
        return queries.find(workspaceIdentifier, identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public Page<WorkspaceEnvironmentConfigurationOutput> findAll(
            AuthorizationContext context,
            String workspaceIdentifier,
            String lifecycle,
            Pageable pageable) {
        return queries.findAll(workspaceIdentifier, lifecycle, pageable);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DEACTIVATE)
    public void inactivate(
            AuthorizationContext context, String workspaceIdentifier, String identifier) {
        commands.inactivate(workspaceIdentifier, identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DELETE)
    public void quarantine(
            AuthorizationContext context, String workspaceIdentifier, String identifier) {
        commands.quarantine(workspaceIdentifier, identifier);
    }
}
