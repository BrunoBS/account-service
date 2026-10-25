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

@DisplayName("Integração HTTP - Environment Type List")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class EnvironmentTypeListIntegrationTest extends BaseIntegrationTest {
    @Autowired private EnvironmentScenario scenario;
    @Autowired private EnvironmentTypeClient types;
    private final EnvironmentTypeFactory factory = new EnvironmentTypeFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("QUERY - lista tipos por ciclo de vida")
    void listaTipos() {
        types.list("ACTIVE").expectOk().expectSize("", 4);
        types.inactivate("CELL").expectNoContent();
        types.list("INACTIVE").expectOk().expectSize("", 1);
    }
}
