package br.com.portalmanager.platform.workspace.core.applicationconfiguration.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.*;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.model.*;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.operations.ApplicationEnvironmentConfigurationCommandService;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.usecase.operations.ApplicationEnvironmentConfigurationQueryService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ApplicationEnvironmentConfigurationFacade {
    private final ApplicationEnvironmentConfigurationCommandService commands;
    private final ApplicationEnvironmentConfigurationQueryService queries;

    public ApplicationEnvironmentConfigurationFacade(
            ApplicationEnvironmentConfigurationCommandService commands,
            ApplicationEnvironmentConfigurationQueryService queries) {
        this.commands = commands;
        this.queries = queries;
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.CREATE)
    public ApplicationEnvironmentConfigurationOutput create(
            AuthorizationContext context,
            String workspaceIdentifier,
            String applicationIdentifier,
            CreateApplicationEnvironmentConfigurationInput input) {
        return commands.create(workspaceIdentifier, applicationIdentifier, input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.UPDATE)
    public ApplicationEnvironmentConfigurationOutput update(
            AuthorizationContext context,
            String workspaceIdentifier,
            String applicationIdentifier,
            String identifier,
            UpdateApplicationEnvironmentConfigurationInput input) {
        return commands.update(workspaceIdentifier, applicationIdentifier, identifier, input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public ApplicationEnvironmentConfigurationOutput find(
            AuthorizationContext context,
            String workspaceIdentifier,
            String applicationIdentifier,
            String identifier) {
        return queries.find(workspaceIdentifier, applicationIdentifier, identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public Page<ApplicationEnvironmentConfigurationOutput> findAll(
            AuthorizationContext context,
            String workspaceIdentifier,
            String applicationIdentifier,
            String lifecycle,
            Pageable pageable) {
        return queries.findAll(workspaceIdentifier, applicationIdentifier, lifecycle, pageable);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DEACTIVATE)
    public void inactivate(
            AuthorizationContext context,
            String workspaceIdentifier,
            String applicationIdentifier,
            String identifier) {
        commands.inactivate(workspaceIdentifier, applicationIdentifier, identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DELETE)
    public void quarantine(
            AuthorizationContext context,
            String workspaceIdentifier,
            String applicationIdentifier,
            String identifier) {
        commands.quarantine(workspaceIdentifier, applicationIdentifier, identifier);
    }
}
