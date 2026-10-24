package br.com.portalmanager.platform.workspace.entrypoint.application.integration;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.workspace.entrypoint.application.ApplicationClient;
import br.com.portalmanager.platform.workspace.entrypoint.application.ApplicationFactory;
import br.com.portalmanager.platform.workspace.entrypoint.application.ApplicationScenario;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.application_scope.ApplicationScopeTypeClient;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.onboarding.OnboardingTypeClient;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.workspace.WorkspaceTypeClient;
import br.com.portalmanager.platform.workspace.entrypoint.schema.SchemaScenario;
import br.com.portalmanager.platform.workspace.entrypoint.workspace.support.WorkspaceClient;
import br.com.portalmanager.platform.workspace.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Application Summary")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class ApplicationSummaryIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private WorkspaceClient workspaces;
    @Autowired private WorkspaceTypeClient workspaceTypes;
    @Autowired private OnboardingTypeClient onboardingTypes;
    @Autowired private ApplicationScopeTypeClient applicationScopes;
    @Autowired private SchemaScenario schemas;
    @Autowired private ApplicationClient applications;
    private final ApplicationFactory factory = new ApplicationFactory();
    private ApplicationScenario scenario;

    @BeforeEach void prepareScenario() {
        scenario = new ApplicationScenario(authorization, workspaces, workspaceTypes, onboardingTypes, applicationScopes, schemas).setup();
    }

    @Test
    @DisplayName("QUERY - retorna resumo das aplicações ativas do workspace")
    void retornaResumoDasAplicacoesAtivas() {
        var active = applications.create(scenario.workspaceIdentifier(), factory.valid("Active application"))
                .expectCreated()
                .extractBody();

        var inactive = applications.create(scenario.workspaceIdentifier(), factory.valid("Inactive application"))
                .expectCreated()
                .extractBody();

        applications.inactivate(scenario.workspaceIdentifier(), inactive.identifier()).expectNoContent();

        applications.summary(scenario.workspaceIdentifier())
                .expectOk()
                .expectContainsItem("identifier", active.identifier())
                .expectNotContainsItem("identifier", inactive.identifier());
    }

    @Test
    @DisplayName("AUTH - limita o resumo às aplicações visíveis para os grupos autorizados")
    void limitaResumoAsAplicacoesVisiveis() {
        String workspace = scenario.createManagerWorkspace("Visible application summary", "PARENT_A");
        var teamA = applications.create(workspace, factory.withAuthorizerGroup("TEAM_A")).expectCreated().extractBody();
        var teamB = applications.create(workspace, factory.withAuthorizerGroup("TEAM_B")).expectCreated().extractBody();
        authorization.reset();

        authorization.allow(session -> session.groups("USER")
                .addAuthorizerGroup("GRP_WORKSPACE_DEV_PARENT_A", "DEV", "DEV", "PARENT_A")
                .addAuthorizerGroup("GRP_APPLICATION_DEV_TEAM_A", "DEV", "DEV", "A-TEAM_A"));

        applications.summary(workspace)
                .expectOk()
                .expectContainsItem("identifier", teamA.identifier())
                .expectNotContainsItem("identifier", teamB.identifier());
        authorization.verifyCalledWithPolicy("DEV");
    }
}
