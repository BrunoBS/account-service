package br.com.portalmanager.platform.workspace.entrypoint.environment.workspace.integration;

import br.com.portalmanager.platform.workspace.entrypoint.environment.support.EnvironmentFactory;
import br.com.portalmanager.platform.workspace.entrypoint.environment.support.EnvironmentUpdateBuilder;
import br.com.portalmanager.platform.workspace.entrypoint.environment.workspace.support.WorkspaceEnvironmentClient;
import br.com.portalmanager.platform.workspace.entrypoint.environment.support.EnvironmentScenario;
import br.com.portalmanager.platform.workspace.support.BaseIntegrationTest;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Workspace Environment Create")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class WorkspaceEnvironmentCreateIntegrationTest extends BaseIntegrationTest {
    @Autowired private EnvironmentScenario scenario;
    @Autowired private WorkspaceEnvironmentClient environments;
    private final EnvironmentFactory factory = new EnvironmentFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("CREATE - cria ambiente de workspace com os campos padrão")
    void criaAmbienteDeWorkspace() {
        environments.create(scenario.workspaceIdentifier(), factory.valid())
                .expectCreated()
                .expect("environmentTypeCode", "CUSTOM")
                .expect("lifecycle", "ACTIVE")
                .expect("workspaceIdentifier", scenario.workspaceIdentifier());
    }

    @Test
    @DisplayName("VALIDATION - rejeita campos obrigatórios ausentes e tipo desconhecido")
    void rejeitaPayloadInvalido() {
        environments.create(scenario.workspaceIdentifier(), factory.withoutName()).expectBadRequest();
        environments.create(scenario.workspaceIdentifier(), factory.withInvalidType()).expectBadRequest();
    }

    @Test
    @DisplayName("VALIDATION - rejeita nome duplicado no mesmo workspace")
    void rejeitaNomeDuplicado() {
        environments.create(scenario.workspaceIdentifier(), factory.valid("Duplicated environment")).expectCreated();
        environments.create(scenario.workspaceIdentifier(), factory.valid("Duplicated environment"))
                .expectBadRequest()
                .expectContainsItem("details.field", "name");
    }
}
