package br.com.portalmanager.platform.workspace.entrypoint.environment.defaults.integration;

import br.com.portalmanager.platform.workspace.entrypoint.environment.support.EnvironmentFactory;
import br.com.portalmanager.platform.workspace.entrypoint.environment.support.EnvironmentUpdateBuilder;
import br.com.portalmanager.platform.workspace.entrypoint.environment.defaults.support.DefaultEnvironmentClient;
import br.com.portalmanager.platform.workspace.entrypoint.environment.support.EnvironmentScenario;
import br.com.portalmanager.platform.workspace.support.BaseIntegrationTest;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Default Environment Create")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class DefaultEnvironmentCreateIntegrationTest extends BaseIntegrationTest {
    @Autowired private EnvironmentScenario scenario;
    @Autowired private DefaultEnvironmentClient defaults;
    private final EnvironmentFactory factory = new EnvironmentFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("CREATE - cria um ambiente global sem workspace")
    void criaAmbienteGlobal() {
        defaults.create(factory.validDefault())
                .expectCreated()
                .expect("environmentTypeCode", "DEFAULT")
                .expect("workspaceIdentifier", null);
    }

    @Test
    @DisplayName("VALIDATION - exige descrição para criar ambiente global")
    void exigeDescricao() {
        defaults.create(factory.withoutDescription()).expectBadRequest();
    }

    @Test
    @DisplayName("VALIDATION - mantém nomes globais únicos")
    void rejeitaNomeGlobalDuplicado() {
        defaults.create(factory.validDefault("Unique global environment")).expectCreated();
        defaults.create(factory.validDefault("Unique global environment"))
                .expectBadRequest()
                .expectContainsItem("details.field", "name");
    }
}
