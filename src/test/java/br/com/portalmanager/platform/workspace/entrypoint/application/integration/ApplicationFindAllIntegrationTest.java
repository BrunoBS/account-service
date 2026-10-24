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

@DisplayName("Integração HTTP - Application FindAll")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class ApplicationFindAllIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("QUERY - filtra por lifecycle e tag, pagina e ordena aplicações")
    void filtraPaginaEOrdenaAplicacoes() {
        var first = applications.create(scenario.workspaceIdentifier(), factory.valid("Application First"))
                .expectCreated().extractBody();
        var second = applications.create(scenario.workspaceIdentifier(), factory.valid("Application Second"))
                .expectCreated().extractBody();
        var third = applications.create(scenario.workspaceIdentifier(), factory.valid("Application Third"))
                .expectCreated().extractBody();
        applications.inactivate(scenario.workspaceIdentifier(), second.identifier()).expectNoContent();

        applications.findAll(scenario.workspaceIdentifier()).lifecycle("ACTIVE").tagName("managed").size(1)
                .sort("name,asc").execute().expectOk().expectContainsItem("content.identifier", first.identifier())
                .expectNotContainsItem("content.identifier", second.identifier()).expect("page.totalElements", 2);
        applications.findAll(scenario.workspaceIdentifier()).lifecycle("INACTIVE").execute().expectOk()
                .expectContainsItem("content.identifier", second.identifier());
        applications.findAll(scenario.workspaceIdentifier()).page(1).size(1).sort("name,asc").execute().expectOk()
                .expect("content[0].identifier", third.identifier()).expect("page.totalPages", 2);
        applications.findAll(scenario.workspaceIdentifier()).sort("id,desc").execute().expectOk()
                .expect("content[0].identifier", third.identifier())
                .expect("content[1].identifier", first.identifier());
    }

    @Test
    @DisplayName("QUERY - usa lifecycle ativo por padrão e retorna lista vazia quando não há aplicações")
    void usaLifecycleAtivoPorPadrao() {
        applications.findAll(scenario.workspaceIdentifier()).execute().expectOk()
                .expectSize("content", 0)
                .expect("page.totalElements", 0);

        var active = applications.create(scenario.workspaceIdentifier(), factory.valid("Active application"))
                .expectCreated()
                .extractBody();
        var inactive = applications.create(scenario.workspaceIdentifier(), factory.valid("Inactive application"))
                .expectCreated()
                .extractBody();
        applications.inactivate(scenario.workspaceIdentifier(), inactive.identifier()).expectNoContent();

        applications.findAll(scenario.workspaceIdentifier()).execute().expectOk()
                .expectSize("content", 1)
                .expectContainsItem("content.identifier", active.identifier())
                .expectNotContainsItem("content.identifier", inactive.identifier());
    }

    @Test
    @DisplayName("VALIDATION - rejeita ordenação por campo não suportado")
    void rejeitaOrdenacaoPorCampoNaoSuportado() {
        applications.findAll(scenario.workspaceIdentifier()).sort("settings").execute().expectBadRequest()
                .expect("code", "APPLICATION-0109");
    }

    @Test
    @DisplayName("VALIDATION - rejeita filtro de lifecycle inválido")
    void rejeitaFiltroLifecycleInvalido() {
        applications.findAll(scenario.workspaceIdentifier()).lifecycle("UNKNOWN").execute().expectBadRequest()
                .expect("code", "LIFECYCLE-0101");
    }

    @Test
    @DisplayName("AUTH - lista somente aplicações visíveis para os grupos autorizados")
    void listaSomenteAplicacoesVisiveis() {
        String workspace = scenario.createManagerWorkspace("Visible application list", "PARENT_A");
        var teamA = applications.create(workspace, factory.withAuthorizerGroup("TEAM_A")).expectCreated().extractBody();
        var teamB = applications.create(workspace, factory.withAuthorizerGroup("TEAM_B")).expectCreated().extractBody();
        authorization.reset();
        authorization.allow(session -> session.groups("USER")
                .addAuthorizerGroup("GRP_WORKSPACE_DEV_PARENT_A", "DEV", "DEV", "PARENT_A")
                .addAuthorizerGroup("GRP_APPLICATION_DEV_TEAM_A", "DEV", "DEV", "A-TEAM_A"));

        applications.findAll(workspace).execute().expectOk().expectSize("content", 1)
                .expect("content[0].identifier", teamA.identifier())
                .expectNotContainsItem("content.identifier", teamB.identifier()).expect("page.totalElements", 1);
        authorization.verifyCalledWithPolicy("DEV");
    }
}
