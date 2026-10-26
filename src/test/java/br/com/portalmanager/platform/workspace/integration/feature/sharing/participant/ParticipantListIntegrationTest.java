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

class ParticipantListIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("QUERY - lista solicitações pendentes da aplicação")
    void listaSolicitacoesPendentes() {
        var contract = scenario.createContract(data);
                scenario.requestParticipation(data, contract.identifier(), participants);
                participants.list(data.participantWorkspace(), data.participantApplication(), "PENDING",
                                contract.identifier(), 0, 20)
                        .expectOk().expectSize("content", 1);
    }

    @Test
    @DisplayName("VALIDATION - rejeita status desconhecido no filtro")
    void rejeitaStatusDesconhecido() {
        participants.list(data.participantWorkspace(), data.participantApplication(),
                                "UNKNOWN", null, 0, 20)
                        .expectBadRequest();
    }
}
