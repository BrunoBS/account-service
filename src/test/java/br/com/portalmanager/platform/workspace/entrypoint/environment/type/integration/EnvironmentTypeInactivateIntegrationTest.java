package br.com.portalmanager.platform.workspace.entrypoint.environment.type.integration;

import br.com.portalmanager.platform.workspace.entrypoint.environment.type.support.EnvironmentTypeClient;
import br.com.portalmanager.platform.workspace.entrypoint.environment.type.support.EnvironmentTypeFactory;
import br.com.portalmanager.platform.workspace.entrypoint.environment.support.EnvironmentScenario;
import br.com.portalmanager.platform.workspace.support.BaseIntegrationTest;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Environment Type Inactivate")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class EnvironmentTypeInactivateIntegrationTest extends BaseIntegrationTest {
    @Autowired private EnvironmentScenario scenario;
    @Autowired private EnvironmentTypeClient types;
    private final EnvironmentTypeFactory factory = new EnvironmentTypeFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("LIFECYCLE - inativa tipo de ambiente")
    void inativaTipo() {
        types.inactivate("CELL").expectNoContent();
        types.find("CELL").expectOk().expect("lifecycle", "INACTIVE");
    }

    @Test
    @DisplayName("NOT FOUND - rejeita inativação de tipo inexistente")
    void rejeitaInativacaoDeTipoInexistente() {
        types.inactivate("UNKNOWN").expectNotFound();
    }
}
