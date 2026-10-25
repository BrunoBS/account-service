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

@DisplayName("Integração HTTP - Default Environment Update")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class DefaultEnvironmentUpdateIntegrationTest extends BaseIntegrationTest {
    @Autowired private EnvironmentScenario scenario;
    @Autowired private DefaultEnvironmentClient defaults;
    private final EnvironmentFactory factory = new EnvironmentFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("UPDATE - atualiza ambiente global com a versão informada")
    void atualizaAmbienteGlobal() {
        var created = defaults.create(factory.validDefault()).expectCreated().extractBody();
        var update = EnvironmentUpdateBuilder.builder()
                .withVersion(created.version())
                .withName("Updated global environment")
                .withAuthorizationType("ADM")
                .withEnvironmentType("DEFAULT")
                .withSettings(created.settings())
                .build();

        defaults.update(created.identifier(), update)
                .expectOk()
                .expect("name", "Updated global environment");
    }

    @Test
    @DisplayName("VALIDATION - rejeita versão obsoleta e nome ausente")
    void rejeitaVersaoObsoletaEPayloadInvalido() {
        var created = defaults.create(factory.validDefault()).expectCreated().extractBody();
        var update = EnvironmentUpdateBuilder.builder()
                .withVersion(created.version())
                .withName("Updated global environment")
                .withAuthorizationType("ADM")
                .withEnvironmentType("DEFAULT")
                .withSettings(created.settings())
                .build();
        defaults.update(created.identifier(), update).expectOk();
        defaults.update(created.identifier(), update).expectConflict();
        defaults.update(created.identifier(), EnvironmentUpdateBuilder.builder()
                        .withVersion(created.version() + 1)
                        .withName(null)
                        .withEnvironmentType("DEFAULT")
                        .build())
                .expectBadRequest();
    }
}
