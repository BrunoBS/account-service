package br.com.portalmanager.platform.workspace.integration.foundation.schema.support;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.fixture.scenario.TestScenario;
import br.com.portalmanager.platform.workspace.entrypoint.workspace.response.WorkspaceResponse;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceFactory;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceScenario;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.onboardingphasetype.support.OnboardingPhaseFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.onboardingphasetype.support.OnboardingTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.workspacetype.support.WorkspaceTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.workspacetype.support.WorkspaceTypeFactory;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Prepares schema catalog prerequisites and optional workspace scope through HTTP clients. */
@Component
public final class SchemaIntegrationScenario implements TestScenario<SchemaIntegrationScenario> {
    private final AuthorizationMock authorization;
    private final SchemaScenario schemas;
    private final WorkspaceTypeClient workspaceTypes;
    private final WorkspaceTypeFactory workspaceTypeFactory = new WorkspaceTypeFactory();
    private final OnboardingTypeClient onboardingTypes;
    private final OnboardingPhaseFactory onboardingPhaseFactory = new OnboardingPhaseFactory();
    private final WorkspaceClient workspaces;
    private final WorkspaceFactory workspaceFactory = new WorkspaceFactory();

    public SchemaIntegrationScenario(
            AuthorizationMock authorization,
            SchemaScenario schemas,
            WorkspaceTypeClient workspaceTypes,
            OnboardingTypeClient onboardingTypes,
            WorkspaceClient workspaces) {
        this.authorization = Objects.requireNonNull(authorization);
        this.schemas = Objects.requireNonNull(schemas);
        this.workspaceTypes = Objects.requireNonNull(workspaceTypes);
        this.onboardingTypes = Objects.requireNonNull(onboardingTypes);
        this.workspaces = Objects.requireNonNull(workspaces);
    }

    @Override
    public SchemaIntegrationScenario setup() {
        new WorkspaceScenario(authorization, workspaceTypes, workspaceTypeFactory,
                onboardingTypes, onboardingPhaseFactory, schemas).setup();
        return this;
    }

    public WorkspaceResponse createWorkspace() {
        return workspaces.create(workspaceFactory.valid()).expectCreated().extractBody();
    }

    public String platformDefaultSchemaIdentifier() {
        return schemas.platformDefaultSchemaIdentifier();
    }
}
