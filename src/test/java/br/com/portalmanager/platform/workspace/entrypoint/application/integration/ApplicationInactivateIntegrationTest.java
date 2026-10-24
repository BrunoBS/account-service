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

@DisplayName("Integração HTTP - Application Inactivate")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class ApplicationInactivateIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("UPDATE - inativa uma aplicação e remove-a da consulta individual")
    void inativaAplicacao() {
        var created = applications.create(scenario.workspaceIdentifier(), factory.valid()).expectCreated().extractBody();

        applications.inactivate(scenario.workspaceIdentifier(), created.identifier()).expectNoContent();

        applications.findByIdentifier(scenario.workspaceIdentifier(), created.identifier()).expectNotFound();

        applications.findAll(scenario.workspaceIdentifier()).lifecycle("INACTIVE")
                .execute().expectOk()
                .expectContainsItem("content.identifier", created.identifier());
    }

    @Test
    @DisplayName("NOT FOUND - não inativa aplicação inexistente")
    void naoInativaAplicacaoInexistente() {
        applications.inactivate(scenario.workspaceIdentifier(), "00000000-0000-0000-0000-000000000000")
                .expectNotFound()
                .expect("code", "APPLICATION-0001");
    }

    @Test
    @DisplayName("AUTH - exige permissão administrativa para inativar uma aplicação")
    void exigePermissaoAdministrativa() {
        var created = applications.create(scenario.workspaceIdentifier(), factory.valid()).expectCreated().extractBody();
        authorization.forbidden();
        applications.inactivate(scenario.workspaceIdentifier(), created.identifier()).expectForbidden();
        authorization.verifyCalledWithPolicy("ADM");
    }
}
