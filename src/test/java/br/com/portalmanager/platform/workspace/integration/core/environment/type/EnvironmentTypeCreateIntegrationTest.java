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

@DisplayName("Integração HTTP - Environment Type Create")
@WithMySql(excludeTables = {"flyway_schema_history"})
class EnvironmentTypeCreateIntegrationTest extends BaseIntegrationTest {
    @Autowired private EnvironmentScenario scenario;
    @Autowired private EnvironmentTypeClient types;
    private final EnvironmentTypeFactory factory = new EnvironmentTypeFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("VALIDATION - rejeita código de tipo já utilizado")
    void rejeitaCodigoDuplicado() {
        types.create(factory.withCode("CELL"))
                .expectBadRequest()
                .expectContainsItem("details.field", "code");
    }

    @Test
    @DisplayName("VALIDATION - rejeita códigos e combinações de escopo não suportados")
    void rejeitaCodigoOuEscopoNaoSuportado() {
        types.create(factory.withCode("DEFAULT")).expectBadRequest();
        types.create(factory.withCode("UNSUPPORTED")).expectBadRequest();
        types.create(factory.withScope(true, false, false))
                .expectBadRequest()
                .expectContainsItem("details.field", "workspaceRequired");
    }
}
