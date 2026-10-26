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

class ParticipantRequestAgainIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("UPDATE - reenvia solicitação rejeitada")
    void reenviaSolicitacaoRejeitada() {
        var contract = scenario.createContract(data);
                String participant = scenario.requestParticipation(data, contract.identifier(), participants);
                contracts.changeParticipantStatus(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                                participant, new ParticipantFactory().reject())
                        .expectOk();
                participants.requestAgain(data.participantWorkspace(), data.participantApplication(), participant)
                        .expectOk().expect("status", "PENDING");
    }
}
