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

@DisplayName("Integração HTTP - Default Environment Delete")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class DefaultEnvironmentDeleteIntegrationTest extends BaseIntegrationTest {
    @Autowired private EnvironmentScenario scenario;
    @Autowired private DefaultEnvironmentClient defaults;
    private final EnvironmentFactory factory = new EnvironmentFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("LIFECYCLE - exige inativação antes da exclusão do ambiente global")
    void removeSomenteAmbienteGlobalInativo() {
        var created = defaults.create(factory.validDefault()).expectCreated().extractBody();

        defaults.delete(created.identifier()).expectBadRequest();
        defaults.inactivate(created.identifier()).expectNoContent();
        defaults.delete(created.identifier()).expectNoContent();
    }

    @Test
    @DisplayName("NOT FOUND - rejeita exclusão de ambiente global inexistente")
    void rejeitaExclusaoDeAmbienteGlobalInexistente() {
        defaults.delete("missing-default-environment").expectNotFound();
    }
}
