package br.com.portalmanager.platform.workspace.integration.core.environment.compatibility;

import br.com.portalmanager.platform.workspace.entrypoint.environment.response.EnvironmentTypeCompatibilityResponse;
import br.com.portalmanager.platform.workspace.integration.core.environment.compatibility.support.EnvironmentTypeCompatibilityClient;
import br.com.portalmanager.platform.workspace.integration.core.environment.support.EnvironmentFactory;
import br.com.portalmanager.platform.workspace.integration.core.environment.support.EnvironmentScenario;
import br.com.portalmanager.platform.workspace.integration.core.environment.workspace.support.WorkspaceEnvironmentClient;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Environment Type Compatibility Delete")
@WithMySql(excludeTables = {"flyway_schema_history"})
class EnvironmentTypeCompatibilityDeleteIntegrationTest extends BaseIntegrationTest {
    @Autowired private EnvironmentScenario scenario;
    @Autowired private EnvironmentTypeCompatibilityClient compatibilities;
    @Autowired private WorkspaceEnvironmentClient environments;
    private final EnvironmentFactory environmentFactory = new EnvironmentFactory();
    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("LIFECYCLE - exige inativação antes de excluir compatibilidade")
    void removeSomenteCompatibilidadeInativa() {
        var relation = findDefaultToShard();

        compatibilities.delete(relation.identifier()).expectBadRequest();
        compatibilities.inactivate(relation.identifier()).expectNoContent();
        compatibilities.delete(relation.identifier()).expectNoContent();
    }

    @Test
    @DisplayName("NOT FOUND - rejeita exclusão de relação inexistente")
    void rejeitaExclusaoDeCompatibilidadeInexistente() {
        compatibilities.delete("missing-compatibility").expectNotFound();
    }

    @Test
    @DisplayName("VALIDATION - impede excluir compatibilidade usada por ambientes existentes")
    void impedeExclusaoDeCompatibilidadeEmUso() {
        var relation = findCustomToShard();
        var parent = environments.create(scenario.workspaceIdentifier(), environmentFactory.valid()).expectCreated().extractBody();
        environments.create(scenario.workspaceIdentifier(), environmentFactory.shard("Environment compatibility child", parent.identifier())).expectCreated();
        compatibilities.inactivate(relation.identifier()).expectNoContent();
        compatibilities.delete(relation.identifier())
                .expectBadRequest()
                .expectContainsItem("details.field", "identifier");
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

    private EnvironmentTypeCompatibilityResponse findCustomToShard() {
        return compatibilities.list("ACTIVE")
                .expectOk()
                .extractList(EnvironmentTypeCompatibilityResponse.class)
                .stream()
                .filter(value -> value.parentTypeCode().equals("CUSTOM") && value.childTypeCode().equals("SHARD"))
                .findFirst()
                .orElseThrow();
    }

}
