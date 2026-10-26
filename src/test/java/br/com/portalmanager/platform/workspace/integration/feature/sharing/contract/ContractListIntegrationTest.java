package br.com.portalmanager.platform.workspace.integration.feature.sharing.contract;

import br.com.portalmanager.platform.workspace.integration.feature.sharing.support.ContractClient;
import br.com.portalmanager.platform.workspace.integration.feature.sharing.support.ContractFactory;
import br.com.portalmanager.platform.workspace.integration.feature.sharing.support.ParticipantClient;
import br.com.portalmanager.platform.workspace.integration.feature.sharing.support.ParticipantFactory;
import br.com.portalmanager.platform.workspace.integration.feature.sharing.support.ResourceSharingScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ContractListIntegrationTest extends BaseIntegrationTest {
    @Autowired private ResourceSharingScenario scenario;
    @Autowired private ContractClient contracts;
    @Autowired private ParticipantClient participants;
    private final ContractFactory factory = new ContractFactory();
    private ResourceSharingScenario.Data data;

    @BeforeEach
    void prepareScenario() {
        data = scenario.setup();
    }

    @Test
    @DisplayName("QUERY - lista contratos ativos da aplicação proprietária")
    void listaContratosAtivos() {
        var contract = scenario.createContract(data);
                contracts.list(data.ownerWorkspace(), data.ownerApplication(), "ACTIVE", 0, 20)
                        .expectOk().expectContains("content[0].identifier", contract.identifier());
    }

    @Test
    @DisplayName("VALIDATION - rejeita número de página inválido")
    void rejeitaPaginaInvalida() {
        contracts.list(data.ownerWorkspace(), data.ownerApplication(), null, -1, 20)
                        .expectBadRequest();
    }
}
