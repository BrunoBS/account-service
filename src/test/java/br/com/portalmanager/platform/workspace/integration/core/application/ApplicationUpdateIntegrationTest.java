package br.com.portalmanager.platform.workspace.integration.core.application;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.workspace.integration.core.application.support.ApplicationClient;
import br.com.portalmanager.platform.workspace.integration.core.application.support.ApplicationFactory;
import br.com.portalmanager.platform.workspace.integration.core.application.support.ApplicationScenario;
import br.com.portalmanager.platform.workspace.integration.core.application.support.ApplicationUpdateFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.applicationscopetype.support.ApplicationScopeTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.onboardingphasetype.support.OnboardingTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.workspacetype.support.WorkspaceTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaScenario;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceClient;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Application Update")
@WithMySql(excludeTables = {"flyway_schema_history"})
class ApplicationUpdateIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private WorkspaceClient workspaces;
    @Autowired private WorkspaceTypeClient workspaceTypes;
    @Autowired private OnboardingTypeClient onboardingTypes;
    @Autowired private ApplicationScopeTypeClient applicationScopes;
    @Autowired private SchemaScenario schemas;
    @Autowired private ApplicationClient applications;
    private final ApplicationFactory factory = new ApplicationFactory();
    private final ApplicationUpdateFactory updateFactory = new ApplicationUpdateFactory();
    private ApplicationScenario scenario;

    @BeforeEach
    void prepareScenario() {
        scenario = new ApplicationScenario(authorization, workspaces, workspaceTypes, onboardingTypes,
                applicationScopes, schemas).setup();
    }

    @Test
    @DisplayName("UPDATE - atualiza os dados com a versão atual e substitui tags manuais")
    void atualizaComVersaoAtual() {
        var created = applications.create(scenario.workspaceIdentifier(), factory.withAuthorizerGroup("TEAM_A"))
                .expectCreated()
                .extractBody();
        var updated = applications.update(
                scenario.workspaceIdentifier(),
                        created.identifier(),
                        updateFactory.renamedWithManualTag(created.version(), "Updated application", "TEAM_B", "updated-tag"))
                .expectOk()
                .expect("name", "Updated application")
                .expect("authorizerGroup", "A-TEAM_B")
                .expect("tags", List.of("updated-tag"))
                .extractBody();

        applications.findByIdentifier(scenario.workspaceIdentifier(), created.identifier())
                .expectOk()
                .expect("version", updated.version().intValue())
                .expectNotContainsItem("tags", "managed");

        applications.findAll(scenario.workspaceIdentifier()).tagName("A-TEAM_A").execute()
                .expectOk()
                .expectNotContainsItem("content.identifier", created.identifier());

        applications.findAll(scenario.workspaceIdentifier()).tagName("A-TEAM_B").execute()
                .expectOk()
                .expectContainsItem("content.identifier", created.identifier());
    }

    @Test
    @DisplayName("UPDATE - rejeita versão ausente, obsoleta e scope inválido")
    void rejeitaVersaoAusenteObsoletaEEscopoInvalido() {
        var created = applications.create(scenario.workspaceIdentifier(), factory.valid())
                .expectCreated()
                .extractBody();
        applications.update(scenario.workspaceIdentifier(), created.identifier(), updateFactory.renamed(null, "Updated application"))
                .expectBadRequest()
                .expectContainsItem("details.field", "version");

        applications.update(scenario.workspaceIdentifier(), created.identifier(),
                        updateFactory.renamed(created.version() + 1, "Updated application")).expectConflict();

        var latest = applications.findByIdentifier(scenario.workspaceIdentifier(), created.identifier())
                .expectOk()
                .extractBody();

        applications.update(scenario.workspaceIdentifier(), created.identifier(),
                        updateFactory.withScope(latest.version(), "Updated application", "UNKNOWN"))
                .expectBadRequest();
    }

    @Test
    @DisplayName("UPDATE - rejeita alteração de nome duplicado e settings fora do schema")
    void rejeitaNomeDuplicadoESettingsInvalidos() {
        applications.create(scenario.workspaceIdentifier(), factory.valid("First application"))
                .expectCreated();
        var other = applications.create(scenario.workspaceIdentifier(), factory.valid("Second application"))
                .expectCreated()
                .extractBody();

        applications.update(scenario.workspaceIdentifier(), other.identifier(),
                        updateFactory.renamed(other.version(), "First application"))
                .expectBadRequest()
                .expectContainsItem("details.field", "name");

        applications.update(scenario.workspaceIdentifier(), other.identifier(),
                        updateFactory.withInvalidSettings(other.version()))
                .expectBadRequest();
    }

    @Test
    @DisplayName("VALIDATION - rejeita valores acima dos limites durante atualização")
    void rejeitaValoresAcimaDosLimitesNoUpdate() {
        var created = applications.create(scenario.workspaceIdentifier(), factory.valid())
                .expectCreated()
                .extractBody();
        for (var invalid : updateFactory.fieldsAboveMaximumLength(created.version())) {
            applications.update(scenario.workspaceIdentifier(), created.identifier(), invalid.request())
                    .expectBadRequest()
                    .expectContainsItem("details.field", invalid.field());
        }
    }

    @Test
    @DisplayName("AUTH - permite atualizar aplicação visível e oculta aplicação de outro grupo")
    void atualizaSomenteAplicacaoVisivel() {
        String workspace = scenario.createManagerWorkspace("Visible application update", "PARENT_A");
        var teamA = applications.create(workspace, factory.withAuthorizerGroup("TEAM_A"))
                .expectCreated()
                .extractBody();
        var teamB = applications.create(workspace, factory.withAuthorizerGroup("TEAM_B"))
                .expectCreated()
                .extractBody();
        authorization.reset();
        authorization.allow(session -> session.groups("USER")
                .addAuthorizerGroup("GRP_WORKSPACE_DEV_PARENT_A", "DEV", "DEV", "PARENT_A")
                .addAuthorizerGroup("GRP_APPLICATION_DEV_TEAM_A", "DEV", "DEV", "A-TEAM_A"));

        applications.update(workspace, teamA.identifier(),
                        updateFactory.renamed(teamA.version(), "Visible application updated"))
                .expectOk()
                .expect("name", "Visible application updated");
        applications.update(workspace, teamB.identifier(),
                        updateFactory.renamed(teamB.version(), "Hidden application updated"))
                .expectNotFound()
                .expect("code", "APPLICATION-0001");
        authorization.verifyCalledWithPolicy("DEV");
    }
}
