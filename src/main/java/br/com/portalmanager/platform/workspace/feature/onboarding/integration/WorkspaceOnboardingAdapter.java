package br.com.portalmanager.platform.workspace.feature.onboarding.integration;

import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.WorkspaceOnboardingReferenceOutput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceCommandService;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceQueryService;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.port.WorkspaceOnboardingPort;

import org.springframework.stereotype.Component;

@Component
public class WorkspaceOnboardingAdapter implements WorkspaceOnboardingPort {
    private final WorkspaceQueryService queries;
    private final WorkspaceCommandService commands;

    public WorkspaceOnboardingAdapter(
            WorkspaceQueryService queries, WorkspaceCommandService commands) {
        this.queries = queries;
        this.commands = commands;
    }

    @Override
    public WorkspaceOnboardingReference findOnboardingReference(String identifier) {
        WorkspaceOnboardingReferenceOutput reference = queries.findOnboardingReference(identifier);
        return new WorkspaceOnboardingReference(
                reference.id(), reference.workspaceType(), reference.onboarding());
    }

    @Override
    public void completeOnboarding(String identifier) {
        commands.completeOnboarding(identifier);
    }
}
