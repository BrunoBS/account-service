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

@DisplayName("Integração HTTP - Environment Type Update")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class EnvironmentTypeUpdateIntegrationTest extends BaseIntegrationTest {
    @Autowired private EnvironmentScenario scenario;
    @Autowired private EnvironmentTypeClient types;
    private final EnvironmentTypeFactory factory = new EnvironmentTypeFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("UPDATE - atualiza metadados do tipo e incrementa a versão")
    void atualizaTipo() {
        var current = types.find("CELL").expectOk().extractBody();
        var update = factory.updated(current.version());

        types.update("CELL", update).expectOk().expect("name", "Cell updated");
    }

    @Test
    @DisplayName("VALIDATION - rejeita versão obsoleta, escopo inválido e alteração do código")
    void rejeitaVersaoEscopoEAlteracaoDeCodigo() {
        var current = types.find("CELL").expectOk().extractBody();
        var update = factory.updated(current.version());

        types.update("CELL", update).expectOk();
        types.update("CELL", update).expectConflict();
        types.update("CELL", factory.withInvalidScope(current.version() + 1)).expectBadRequest();
        types.update("CELL", factory.withChangedCode(current.version() + 1)).expectBadRequest();
    }
}
