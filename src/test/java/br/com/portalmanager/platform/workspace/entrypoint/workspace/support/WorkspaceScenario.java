package br.com.portalmanager.platform.workspace.entrypoint.workspace.support;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.CatalogScenario;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.onboarding.OnboardingPhaseFactory;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.onboarding.OnboardingTypeClient;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.workspace.WorkspaceTypeFactory;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.workspace.WorkspaceTypeClient;
import br.com.portalmanager.platform.workspace.entrypoint.schema.SchemaScenario;

import java.util.Objects;

/** Prepares the catalog dependencies and authorization used by Workspace HTTP scenarios. */
public final class WorkspaceScenario {
    private final AuthorizationMock authorization;
    private final SchemaScenario schemas;
    private final CatalogScenario catalogs;

    public WorkspaceScenario(
            AuthorizationMock authorization,
            WorkspaceTypeClient workspaceTypes,
            WorkspaceTypeFactory workspaceTypeFactory,
            OnboardingTypeClient onboardingTypes,
            OnboardingPhaseFactory onboardingPhaseFactory,
            SchemaScenario schemas) {
        this.authorization = Objects.requireNonNull(authorization);
        this.schemas = Objects.requireNonNull(schemas);
        this.catalogs = CatalogScenario.builder()
                .withWorkspaceTypes(Objects.requireNonNull(workspaceTypes),
                        Objects.requireNonNull(workspaceTypeFactory).all())
                .withOnboardingPhases(Objects.requireNonNull(onboardingTypes),
                        Objects.requireNonNull(onboardingPhaseFactory).all());
    }

    public WorkspaceScenario setup() {
        authorization.reset();
        authorization.allow(session -> session.userName("owner-it").groups("PM5_OWNER"));
        schemas.setup();
        catalogs.setup();
        return this;
    }
}
