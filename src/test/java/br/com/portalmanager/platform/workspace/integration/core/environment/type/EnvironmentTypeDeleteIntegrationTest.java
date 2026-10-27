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

@DisplayName("Integração HTTP - Environment Type Delete")
@WithMySql(excludeTables = {"flyway_schema_history"})
class EnvironmentTypeDeleteIntegrationTest extends BaseIntegrationTest {
    @Autowired private EnvironmentScenario scenario;
    @Autowired private EnvironmentTypeClient types;
    private final EnvironmentTypeFactory factory = new EnvironmentTypeFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("LIFECYCLE - protege exclusão de tipo utilizado")
    void protegeExclusaoDeTipoEmUso() {
        types.delete("CELL").expectBadRequest();
        types.inactivate("CELL").expectNoContent();
        types.delete("CELL").expectBadRequest();
    }

    @Test
    @DisplayName("NOT FOUND - rejeita exclusão de tipo inexistente")
    void rejeitaExclusaoDeTipoInexistente() {
        types.delete("UNKNOWN").expectNotFound();
    }
}
