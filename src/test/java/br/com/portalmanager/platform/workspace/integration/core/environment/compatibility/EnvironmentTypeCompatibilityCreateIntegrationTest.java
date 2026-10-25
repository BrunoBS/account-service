package br.com.portalmanager.platform.workspace.integration.core.environment.compatibility;

import br.com.portalmanager.platform.workspace.entrypoint.environment.response.EnvironmentTypeCompatibilityResponse;
import br.com.portalmanager.platform.workspace.integration.core.environment.compatibility.support.EnvironmentTypeCompatibilityClient;
import br.com.portalmanager.platform.workspace.integration.core.environment.compatibility.support.EnvironmentTypeCompatibilityFactory;
import br.com.portalmanager.platform.workspace.integration.core.environment.support.EnvironmentScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Integração HTTP - Environment Type Compatibility Create")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class EnvironmentTypeCompatibilityCreateIntegrationTest extends BaseIntegrationTest {
    @Autowired private EnvironmentScenario scenario;
    @Autowired private EnvironmentTypeCompatibilityClient compatibilities;
    private final EnvironmentTypeCompatibilityFactory factory = new EnvironmentTypeCompatibilityFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("CREATE - reativa compatibilidade inativa")
    void reativaCompatibilidadeInativa() {
        var relation = findDefaultToShard();

        compatibilities.inactivate(relation.identifier()).expectNoContent();

        compatibilities.create(factory.validDefaultToShard())
                .expectCreated()
                .expect("lifecycle", "ACTIVE");
    }

    @Test
    @DisplayName("VALIDATION - rejeita relação incompatível entre tipos")
    void rejeitaRelacaoIncompativel() {
        compatibilities.create(factory.incompatibleCellToShard()).expectBadRequest();
    }

    @Test
    @DisplayName("CREATE - mantém a compatibilidade existente ao receber o mesmo par ativo")
    void naoDuplicaCompatibilidadeAtiva() {
        var existing = findDefaultToShard();

        var repeated = compatibilities.create(factory.validDefaultToShard())
                .expectCreated()
                .expect("lifecycle", "ACTIVE");

        assertEquals(existing.identifier(), repeated.identifier());
        compatibilities.list("ACTIVE").expectOk().expectSize("", 3);
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
