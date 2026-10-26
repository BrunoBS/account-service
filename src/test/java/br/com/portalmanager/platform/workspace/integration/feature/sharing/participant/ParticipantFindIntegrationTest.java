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

class ParticipantFindIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("QUERY - busca solicitação da aplicação participante")
    void buscaSolicitacao() {
        var contract = scenario.createContract(data);
                String participant = scenario.requestParticipation(data, contract.identifier(), participants);
                participants.find(data.participantWorkspace(), data.participantApplication(), participant)
                        .expectOk().expect("status", "PENDING");
    }

    @Test
    @DisplayName("QUERY - retorna não encontrado para solicitação ausente")
    void naoEncontraSolicitacaoAusente() {
        participants.find(data.participantWorkspace(), data.participantApplication(),
                                "missing-participant")
                        .expectNotFound();
    }
}
