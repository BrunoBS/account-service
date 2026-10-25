package br.com.portalmanager.platform.workspace.integration.core.application;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.workspace.integration.core.application.support.ApplicationClient;
import br.com.portalmanager.platform.workspace.integration.core.application.support.ApplicationFactory;
import br.com.portalmanager.platform.workspace.integration.core.application.support.ApplicationScenario;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.applicationscopetype.support.ApplicationScopeTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.onboardingphasetype.support.OnboardingTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.workspacetype.support.WorkspaceTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaScenario;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceClient;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Application Restore")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class ApplicationRestoreIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("RESTORE - restaura uma aplicação inativa preservando os dados")
    void restauraAplicacaoInativa() {
        var created = applications.create(scenario.workspaceIdentifier(), factory.withAuthorizerGroup("TEAM_A"))
                .expectCreated()
                .extractBody();

        applications.inactivate(scenario.workspaceIdentifier(), created.identifier()).expectNoContent();

        applications.restore(scenario.workspaceIdentifier(), created.identifier())
                .expectOk()
                .expect("identifier", created.identifier())
                .expect("lifecycle", "ACTIVE")
                .expectContainsItem("tags", "managed");

        applications.findByIdentifier(scenario.workspaceIdentifier(), created.identifier())
                .expectOk()
                .expect("name", created.name());
    }

    @Test
    @DisplayName("RESTORE - rejeita aplicação que já está ativa")
    void rejeitaRestauracaoDeAplicacaoAtiva() {
        var created = applications.create(scenario.workspaceIdentifier(), factory.valid())
                .expectCreated()
                .extractBody();

        applications.restore(scenario.workspaceIdentifier(), created.identifier()).expectBadRequest();
        authorization.verifyCalledWithPolicy("ADM");
    }
}
