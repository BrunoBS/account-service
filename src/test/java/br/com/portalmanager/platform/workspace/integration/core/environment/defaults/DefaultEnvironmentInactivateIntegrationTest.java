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

@DisplayName("Integração HTTP - Default Environment Inactivate")
@WithMySql(excludeTables = {"flyway_schema_history"})
class DefaultEnvironmentInactivateIntegrationTest extends BaseIntegrationTest {
    @Autowired private EnvironmentScenario scenario;
    @Autowired private DefaultEnvironmentClient defaults;
    private final EnvironmentFactory factory = new EnvironmentFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("LIFECYCLE - inativa ambiente global")
    void inativaAmbienteGlobal() {
        var created = defaults.create(factory.validDefault()).expectCreated().extractBody();

        defaults.inactivate(created.identifier()).expectNoContent();
        defaults.list("INACTIVE").expectOk().expectSize("", 1);
    }

    @Test
    @DisplayName("NOT FOUND - rejeita inativação de ambiente global inexistente")
    void rejeitaInativacaoDeAmbienteGlobalInexistente() {
        defaults.inactivate("missing-default-environment").expectNotFound();
    }
}
