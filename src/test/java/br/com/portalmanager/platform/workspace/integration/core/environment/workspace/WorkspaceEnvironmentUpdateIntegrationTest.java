package br.com.portalmanager.platform.workspace.integration.core.environment.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

@DisplayName("Integração HTTP - Workspace Environment Update")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class WorkspaceEnvironmentUpdateIntegrationTest extends BaseIntegrationTest {
    @Autowired private EnvironmentScenario scenario;
    @Autowired private WorkspaceEnvironmentClient environments;
    private final EnvironmentFactory factory = new EnvironmentFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("UPDATE - atualiza o ambiente e incrementa sua versão")
    void atualizaAmbiente() {
        var created = environments.create(scenario.workspaceIdentifier(), factory.valid())
                .expectCreated()
                .extractBody();
        var update = EnvironmentUpdateBuilder.builder()
                .withVersion(created.version())
                .withName("Renamed environment")
                .withAuthorizationType("TST")
                .withSettings(created.settings())
                .build();
        var updated = environments.update(scenario.workspaceIdentifier(), created.identifier(), update)
                .expectOk()
                .expect("name", "Renamed environment")
                .extractBody();

        assertEquals(created.version() + 1, updated.version());
    }

    @Test
    @DisplayName("VALIDATION - rejeita versão obsoleta e nome ausente")
    void rejeitaVersaoObsoletaEPayloadInvalido() {
        var created = environments.create(scenario.workspaceIdentifier(), factory.valid())
                .expectCreated()
                .extractBody();
        var update = EnvironmentUpdateBuilder.builder()
                .withVersion(created.version())
                .withName("Renamed environment")
                .withAuthorizationType("TST")
                .withSettings(created.settings())
                .build();

        environments.update(scenario.workspaceIdentifier(), created.identifier(), update).expectOk();
        environments.update(scenario.workspaceIdentifier(), created.identifier(), update).expectConflict();
        environments.update(scenario.workspaceIdentifier(), created.identifier(),
                EnvironmentUpdateBuilder.builder().withVersion(created.version() + 1).withName(null).build())
                .expectBadRequest();
    }
}
