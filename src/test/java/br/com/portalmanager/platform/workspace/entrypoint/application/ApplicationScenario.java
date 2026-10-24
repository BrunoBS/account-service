package br.com.portalmanager.platform.workspace.entrypoint.application;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.fixture.scenario.TestScenario;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.CatalogScenario;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.application_scope.ApplicationScopeTypeClient;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.application_scope.CatalogFactory;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.onboarding.OnboardingPhaseFactory;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.onboarding.OnboardingTypeClient;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.workspace.WorkspaceTypeClient;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.workspace.WorkspaceTypeFactory;
import br.com.portalmanager.platform.workspace.entrypoint.schema.SchemaFactory;
import br.com.portalmanager.platform.workspace.entrypoint.schema.SchemaScenario;
import br.com.portalmanager.platform.workspace.entrypoint.workspace.support.WorkspaceClient;
import br.com.portalmanager.platform.workspace.entrypoint.workspace.support.WorkspaceFactory;
import br.com.portalmanager.platform.workspace.entrypoint.workspace.support.WorkspaceScenario;
import java.util.List;
import java.util.Objects;

/** Prepares the catalog, schema, authorization and workspace used by Application HTTP scenarios. */
public final class ApplicationScenario implements TestScenario<ApplicationScenario> {
    private final AuthorizationMock authorization;
    private final WorkspaceClient workspaces;
    private final WorkspaceTypeClient workspaceTypes;
    private final OnboardingTypeClient onboardingTypes;
    private final ApplicationScopeTypeClient applicationScopes;
    private final SchemaScenario schemas;
    private final WorkspaceFactory workspaceFactory = new WorkspaceFactory();
    private final WorkspaceTypeFactory workspaceTypeFactory = new WorkspaceTypeFactory();
    private final OnboardingPhaseFactory onboardingFactory = new OnboardingPhaseFactory();
    private final CatalogFactory scopeFactory = new CatalogFactory();
    private final SchemaFactory schemaFactory = new SchemaFactory();
    private String workspaceIdentifier;

    public ApplicationScenario(AuthorizationMock authorization, WorkspaceClient workspaces,
            WorkspaceTypeClient workspaceTypes, OnboardingTypeClient onboardingTypes,
            ApplicationScopeTypeClient applicationScopes, SchemaScenario schemas) {
        this.authorization = Objects.requireNonNull(authorization);
        this.workspaces = Objects.requireNonNull(workspaces);
        this.workspaceTypes = Objects.requireNonNull(workspaceTypes);
        this.onboardingTypes = Objects.requireNonNull(onboardingTypes);
        this.applicationScopes = Objects.requireNonNull(applicationScopes);
        this.schemas = Objects.requireNonNull(schemas);
    }

    /** Adds only Application schemas and catalogs to an already prepared Workspace test scenario. */
    public ApplicationScenario(SchemaScenario schemas, ApplicationScopeTypeClient applicationScopes) {
        this.authorization = null;
        this.workspaces = null;
        this.workspaceTypes = null;
        this.onboardingTypes = null;
        this.applicationScopes = Objects.requireNonNull(applicationScopes);
        this.schemas = Objects.requireNonNull(schemas);
    }

    @Override
    public ApplicationScenario setup() {
        if (workspaces != null) {
            new WorkspaceScenario(authorization, workspaceTypes, workspaceTypeFactory,
                    onboardingTypes, onboardingFactory, schemas).setup();
        }
        schemas.bindResource("APPLICATION", "application", schemaFactory.applicationResourceSchema());
        CatalogScenario.builder()
                .withApplicationScopeTypes(applicationScopes, List.of(scopeFactory.valid("BACKEND")))
                .setup();
        if (workspaces != null) {
            workspaceIdentifier = workspaces.create(workspaceFactory.manager("Application workspace"))
                    .expectCreated().extractBody().identifier();
        }
        return this;
    }

    public String workspaceIdentifier() { return workspaceIdentifier; }

    public String createAdminWorkspace(String name) {
        return workspaces.create(workspaceFactory.admin(name)).expectCreated().extractBody().identifier();
    }

    public String createManagerWorkspace(String name, String authorizerGroup) {
        return workspaces.create(workspaceFactory.managerWithAuthorizerGroup(name, authorizerGroup))
                .expectCreated().extractBody().identifier();
    }
}
