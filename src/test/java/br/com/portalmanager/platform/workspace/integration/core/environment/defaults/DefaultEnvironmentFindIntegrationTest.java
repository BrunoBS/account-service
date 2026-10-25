package br.com.portalmanager.platform.workspace.integration.core.environment.defaults;

import br.com.portalmanager.platform.workspace.integration.core.environment.support.EnvironmentFactory;
import br.com.portalmanager.platform.workspace.integration.core.environment.support.EnvironmentUpdateBuilder;
import br.com.portalmanager.platform.workspace.integration.core.environment.defaults.support.DefaultEnvironmentClient;
import br.com.portalmanager.platform.workspace.integration.core.environment.support.EnvironmentScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Default Environment Find")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class DefaultEnvironmentFindIntegrationTest extends BaseIntegrationTest {
    @Autowired private EnvironmentScenario scenario;
    @Autowired private DefaultEnvironmentClient defaults;
    private final EnvironmentFactory factory = new EnvironmentFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("QUERY - busca ambiente global pelo identificador")
    void buscaAmbienteGlobal() {
        var created = defaults.create(factory.validDefault()).expectCreated().extractBody();

        defaults.find(created.identifier()).expectOk().expect("identifier", created.identifier());
        defaults.find("missing-default-environment").expectNotFound();
    }
}
