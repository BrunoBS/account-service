package br.com.portalmanager.platform.workspace.integration.core.workspace;

import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceClient;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceFactory;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceScenario;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.onboardingphasetype.support.OnboardingPhaseFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.onboardingphasetype.support.OnboardingTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.workspacetype.support.WorkspaceTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.workspacetype.support.WorkspaceTypeFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Workspace FindAll")
@WithMySql(excludeTables = {
        "flyway_schema_history",
        "type_life_cycle",
        "type_schema_scopes",
        "type_schema_version_status"
})
class WorkspaceFindAllIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private WorkspaceTypeClient workspaceTypes;
    @Autowired private OnboardingTypeClient onboardingTypes;
    @Autowired private WorkspaceClient workspaces;
    @Autowired private SchemaScenario schemaScenario;

    private final WorkspaceFactory workspaceFactory = new WorkspaceFactory();
    private final WorkspaceTypeFactory workspaceTypeFactory = new WorkspaceTypeFactory();
    private final OnboardingPhaseFactory onboardingPhaseFactory = new OnboardingPhaseFactory();

    @BeforeEach
    void prepareWorkspaceScenario() {
        new WorkspaceScenario(authorization, workspaceTypes, workspaceTypeFactory,
                onboardingTypes, onboardingPhaseFactory, schemaScenario).setup();
    }

    @Test
    @DisplayName("QUERY - filtra workspaces por tipo e lifecycle")
    void filtraWorkspacesPorTipoELifecycle() {
        var admin = workspaces.create(workspaceFactory.admin("Workspace Admin"))
                .expectCreated().extractBody();
        var manager = workspaces.create(workspaceFactory.manager("Workspace Manager"))
                .expectCreated().extractBody();
        workspaces.inactivate(manager.identifier()).expectNoContent();

        workspaces.findAll().typeName(" admin ").execute()
                .expectOk()
                .expectContainsItem("content.identifier", admin.identifier())
                .expectNotContainsItem("content.identifier", manager.identifier())
                .expect("content[0].workspaceType", "ADMIN");
        workspaces.findAll().lifecycle("INACTIVE").execute()
                .expectOk()
                .expect("content[0].identifier", manager.identifier())
                .expect("content[0].lifecycle", "INACTIVE");
    }

    @Test
    @DisplayName("QUERY - pagina e ordena resultados sem truncar aprovadores")
    void paginaEOrdenaResultados() {
        for (int index = 0; index < 3; index++) {
            workspaces.create(workspaceFactory.admin("Workspace Página " + index))
                    .expectCreated();
        }

        workspaces.findAll().page(0).size(2).execute()
                .expectOk()
                .expectSize("content", 2)
                .expect("page.number", 0)
                .expect("page.size", 2)
                .expect("page.totalElements", 3)
                .expect("page.totalPages", 2);
        workspaces.findAll().page(1).size(2).execute()
                .expectOk()
                .expectSize("content", 1)
                .expect("content[0].name", "Workspace Página 2");
        workspaces.findAll().sort("name,desc").size(2).execute()
                .expectOk()
                .expect("content[0].name", "Workspace Página 2");
    }

    @Test
    @DisplayName("VALIDATION - rejeita filtro por tipo de workspace inválido")
    void rejeitaFiltroPorTipoInvalido() {
        workspaces.findAll().typeName("INVALID").execute()
                .expectBadRequest()
                .expect("code", "GLOBAL-0001")
                .expectContains("details[0].field", "typeName");
        workspaces.findAll().typeName("CATALOG").execute().expectBadRequest();
    }

    @Test
    @DisplayName("QUERY - normaliza filtro por tag e valida parâmetros de paginação e ordenação")
    void normalizaFiltroTagEValidaPaginacao() {
        for (int index = 0; index < 3; index++) {
            workspaces.create(workspaceFactory.admin("Workspace Page " + index))
                    .expectCreated();
        }
        workspaces.findAll().page("-1").size("0").execute().expectOk()
                .expect("page.number", 0).expect("page.size", 20);
        workspaces.findAll().page("text").size("text").execute().expectOk()
                .expect("page.number", 0).expect("page.size", 20);
        workspaces.findAll().page("2147483648").execute().expectOk()
                .expect("page.number", 0);
        workspaces.findAll().size(101).execute().expectOk().expect("page.size", 100);
        workspaces.findAll().page(2147483647).size(100).execute().expectBadRequest();
        workspaces.findAll().sort("approvers.email").execute().expectBadRequest();
        workspaces.findAll().sort("unknown").execute().expectBadRequest();
        workspaces.findAll().sort("name,desc").size(2).execute().expectOk()
                .expect("content[0].name", "Workspace Page 2")
                .expect("content[1].name", "Workspace Page 1");
    }

    @Test
    @DisplayName("AUTH - pagina, conta e filtra somente recursos visíveis")
    void paginaContaEFiltraRecursosVisiveis() {
        for (int index = 0; index < 3; index++) {
            workspaces.create(workspaceFactory.withManualTags(
                    "Workspace Visible " + index, "WSP", "TEAM_A", List.of("common-tag")))
                    .expectCreated();
        }
        workspaces.create(workspaceFactory.withManualTags(
                        "Workspace Hidden", "WSP", "TEAM_B", List.of("common-tag")))
                .expectCreated();
        allowTeamAAdministrator();

        workspaces.findAll().page(0).size(1).tagName("common-tag").execute().expectOk()
                .expectSize("content", 1)
                .expect("page.totalElements", 3)
                .expect("page.totalPages", 3)
                .expect("content[0].authorizerGroup", "TEAM_A");
        workspaces.findAll().page(2).size(1).tagName("common-tag").execute().expectOk()
                .expectSize("content", 1).expect("page.totalElements", 3);
        workspaces.findAll().page(3).size(1).tagName("common-tag").execute().expectOk()
                .expectSize("content", 0).expect("page.totalElements", 3);
    }

    @Test
    @DisplayName("AUTH - restringe a listagem à visibilidade do grupo")
    void limitaListagemPorVisibilidadeDoGrupo() {
        var teamA = workspaces.create(workspaceFactory.adminWithAuthorizerGroup(
                        "Workspace List Team A", "TEAM_A"))
                .expectCreated().extractBody();
        workspaces.create(workspaceFactory.adminWithAuthorizerGroup(
                        "Workspace List Team B", "TEAM_B"))
                .expectCreated();
        authorization.reset();
        authorization.allow(session -> session.userName("test-user").groups("USER")
                .addAuthorizerGroup("GRP_WORKSPACE_DEV_TEAM_A", "DEV", "DEV", "TEAM_A"));

        workspaces.findAll().execute().expectOk()
                .expectSize("content", 1)
                .expect("content[0].identifier", teamA.identifier())
                .expect("page.totalElements", 1);
        authorization.verifyCalledWithPolicy("OPEN");
    }

    private void allowTeamAAdministrator() {
        authorization.reset();
        authorization.allow(session -> session.userName("test-user").groups("USER")
                .addAuthorizerGroup("GRP_WORKSPACE_ADM_TEAM_A", "ADM", "ADM", "TEAM_A"));
    }
}
