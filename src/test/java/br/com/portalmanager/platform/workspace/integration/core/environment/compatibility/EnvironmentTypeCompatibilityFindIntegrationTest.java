package br.com.portalmanager.platform.workspace.integration.core.environment.compatibility;

import br.com.portalmanager.platform.workspace.entrypoint.environment.response.EnvironmentTypeCompatibilityResponse;
import br.com.portalmanager.platform.workspace.integration.core.environment.compatibility.support.EnvironmentTypeCompatibilityClient;
import br.com.portalmanager.platform.workspace.integration.core.environment.support.EnvironmentScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Environment Type Compatibility Find")
@WithMySql(excludeTables = {"flyway_schema_history"})
class EnvironmentTypeCompatibilityFindIntegrationTest extends BaseIntegrationTest {
    @Autowired private EnvironmentScenario scenario;
    @Autowired private EnvironmentTypeCompatibilityClient compatibilities;
    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("QUERY - busca uma relação existente entre tipos")
    void buscaCompatibilidade() {
        var relation = findDefaultToShard();

        compatibilities.find(relation.identifier())
                .expectOk()
                .expect("parentTypeCode", "DEFAULT")
                .expect("childTypeCode", "SHARD");
    }

    @Test
    @DisplayName("NOT FOUND - rejeita identificador de relação inexistente")
    void rejeitaIdentificadorInexistente() {
        compatibilities.find("missing-compatibility").expectNotFound();
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
