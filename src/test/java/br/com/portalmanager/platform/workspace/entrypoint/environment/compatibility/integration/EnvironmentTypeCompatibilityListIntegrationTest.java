package br.com.portalmanager.platform.workspace.entrypoint.environment.compatibility.integration;

import br.com.portalmanager.platform.workspace.entrypoint.environment.response.EnvironmentTypeCompatibilityResponse;
import br.com.portalmanager.platform.workspace.entrypoint.environment.compatibility.support.EnvironmentTypeCompatibilityClient;
import br.com.portalmanager.platform.workspace.entrypoint.environment.support.EnvironmentScenario;
import br.com.portalmanager.platform.workspace.support.BaseIntegrationTest;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Environment Type Compatibility List")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class EnvironmentTypeCompatibilityListIntegrationTest extends BaseIntegrationTest {
    @Autowired private EnvironmentScenario scenario;
    @Autowired private EnvironmentTypeCompatibilityClient compatibilities;
    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("QUERY - lista todas as compatibilidades sem filtro")
    void listaTodasAsCompatibilidades() {
        compatibilities.list().expectOk().expectSize("", 3);
    }

    @Test
    @DisplayName("QUERY - filtra compatibilidades ativas")
    void listaCompatibilidadesAtivas() {
        compatibilities.list("ACTIVE").expectOk().expectSize("", 3);
    }

    @Test
    @DisplayName("QUERY - filtra compatibilidades inativas")
    void listaCompatibilidadesInativas() {
        var relation = findDefaultToShard();
        compatibilities.inactivate(relation.identifier()).expectNoContent();
        compatibilities.list("INACTIVE").expectOk().expectSize("", 1);
    }

    private EnvironmentTypeCompatibilityResponse findDefaultToShard() {
        return compatibilities.list("ACTIVE")
                .expectOk()
                .extractList(EnvironmentTypeCompatibilityResponse.class)
                .stream()
                .filter(value -> value.parentTypeCode().equals("DEFAULT") && value.childTypeCode().equals("SHARD"))
                .findFirst()
                .orElseThrow();
    }
}
