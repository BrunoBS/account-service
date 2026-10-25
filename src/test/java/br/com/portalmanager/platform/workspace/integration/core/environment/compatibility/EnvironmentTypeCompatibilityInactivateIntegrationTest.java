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

@DisplayName("Integração HTTP - Environment Type Compatibility Inactivate")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class EnvironmentTypeCompatibilityInactivateIntegrationTest extends BaseIntegrationTest {
    @Autowired private EnvironmentScenario scenario;
    @Autowired private EnvironmentTypeCompatibilityClient compatibilities;
    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("LIFECYCLE - inativa compatibilidade entre tipos")
    void inativaCompatibilidade() {
        var relation = findDefaultToShard();

        compatibilities.inactivate(relation.identifier()).expectNoContent();
        compatibilities.find(relation.identifier()).expectOk().expect("lifecycle", "INACTIVE");
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

    @Test
    @DisplayName("NOT FOUND - rejeita inativação de relação inexistente")
    void rejeitaInativacaoDeCompatibilidadeInexistente() {
        compatibilities.inactivate("missing-compatibility").expectNotFound();
    }
}
