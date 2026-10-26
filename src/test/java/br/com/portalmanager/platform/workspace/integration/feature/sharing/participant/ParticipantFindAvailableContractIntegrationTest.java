package br.com.portalmanager.platform.workspace.integration.feature.sharing.participant;

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

class ParticipantFindAvailableContractIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("QUERY - busca contrato disponível para participação")
    void buscaContratoDisponivel() {
        var contract = scenario.createContract(data);
                participants.findContract(data.participantWorkspace(), data.participantApplication(), contract.identifier())
                        .expectOk().expect("identifier", contract.identifier());
    }

    @Test
    @DisplayName("QUERY - retorna não encontrado para contrato indisponível")
    void naoEncontraContratoIndisponivel() {
        participants.findContract(data.participantWorkspace(), data.participantApplication(),
                                "missing-contract")
                        .expectNotFound();
    }
}
