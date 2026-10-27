package br.com.portalmanager.platform.workspace.integration.core.environment.workspace;

import br.com.portalmanager.platform.workspace.integration.core.environment.support.EnvironmentFactory;
import br.com.portalmanager.platform.workspace.integration.core.environment.support.EnvironmentUpdateBuilder;
import br.com.portalmanager.platform.workspace.integration.core.environment.workspace.support.WorkspaceEnvironmentClient;
import br.com.portalmanager.platform.workspace.integration.core.environment.support.EnvironmentScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Workspace Environment Inactivate")
@WithMySql(excludeTables = {"flyway_schema_history"})
class WorkspaceEnvironmentInactivateIntegrationTest extends BaseIntegrationTest {
    @Autowired private EnvironmentScenario scenario;
    @Autowired private WorkspaceEnvironmentClient environments;
    private final EnvironmentFactory factory = new EnvironmentFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("LIFECYCLE - inativa ambiente e remove-o da consulta ativa")
    void inativaAmbiente() {
        var created = environments.create(scenario.workspaceIdentifier(), factory.valid())
                .expectCreated()
                .extractBody();

        environments.inactivate(scenario.workspaceIdentifier(), created.identifier()).expectNoContent();
        environments.list(scenario.workspaceIdentifier(), "INACTIVE").expectOk().expectSize("", 1);
        environments.find(scenario.workspaceIdentifier(), created.identifier()).expectNotFound();
    }

    @Test
    @DisplayName("NOT FOUND - rejeita inativação de ambiente inexistente")
    void rejeitaInativacaoDeAmbienteInexistente() {
        environments.inactivate(scenario.workspaceIdentifier(), "missing-environment").expectNotFound();
    }
}
