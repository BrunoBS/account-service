package br.com.portalmanager.platform.workspace.integration.core.environment.type;

import br.com.portalmanager.platform.workspace.integration.core.environment.type.support.EnvironmentTypeClient;
import br.com.portalmanager.platform.workspace.integration.core.environment.type.support.EnvironmentTypeFactory;
import br.com.portalmanager.platform.workspace.integration.core.environment.support.EnvironmentScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Environment Type Restore")
@WithMySql(excludeTables = {"flyway_schema_history"})
class EnvironmentTypeRestoreIntegrationTest extends BaseIntegrationTest {
    @Autowired private EnvironmentScenario scenario;
    @Autowired private EnvironmentTypeClient types;
    private final EnvironmentTypeFactory factory = new EnvironmentTypeFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("LIFECYCLE - restaura tipo de ambiente")
    void restauraTipo() {
        types.inactivate("CELL").expectNoContent();

        types.restore("CELL").expectOk().expect("lifecycle", "ACTIVE");
    }

    @Test
    @DisplayName("NOT FOUND - rejeita restauração de tipo inexistente")
    void rejeitaRestauracaoDeTipoInexistente() {
        types.restore("UNKNOWN").expectNotFound();
    }
}
