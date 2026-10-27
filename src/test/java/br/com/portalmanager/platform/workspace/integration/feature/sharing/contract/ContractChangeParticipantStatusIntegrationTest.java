package br.com.portalmanager.platform.workspace.integration.feature.sharing.contract;

import br.com.portalmanager.platform.workspace.integration.feature.sharing.support.ContractClient;
import br.com.portalmanager.platform.workspace.integration.feature.sharing.support.ContractFactory;
import br.com.portalmanager.platform.workspace.integration.feature.sharing.support.ParticipantClient;
import br.com.portalmanager.platform.workspace.integration.feature.sharing.support.ParticipantFactory;
import br.com.portalmanager.platform.workspace.integration.feature.sharing.support.ResourceSharingScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ContractChangeParticipantStatusIntegrationTest extends BaseIntegrationTest {
    @Autowired private ResourceSharingScenario scenario;
    @Autowired private ContractClient contracts;
    @Autowired private ParticipantClient participants;
    @Autowired private AuthorizationMock authorization;
    private final ContractFactory factory = new ContractFactory();
    private ResourceSharingScenario.Data data;

    @BeforeEach
    void prepareScenario() {
        data = scenario.setup();
    }

    @Test
    @DisplayName("UPDATE - rejeita uma solicitação pendente")
    void rejeitaSolicitacaoPendente() {
        ContractResponse contract = scenario.createContract(data);
                String participant = scenario.requestParticipation(data, contract.identifier(), participants);
                contracts.changeParticipantStatus(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                                participant, new ParticipantFactory().reject())
                        .expectOk().expect("status", "REJECTED");
    }

    @Test
    @DisplayName("VALIDATION - rejeita ação de status desconhecida")
    void rejeitaAcaoDesconhecida() {
        ContractResponse contract = scenario.createContract(data);
                String participant = scenario.requestParticipation(data, contract.identifier(), participants);
                contracts.changeParticipantStatus(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                                participant, new ParticipantFactory().withInvalidAction())
                        .expectBadRequest();
    }

    @Test
    @DisplayName("VALIDATION - rejeita alteração sem ação")
    void rejeitaAcaoAusente() {
        ContractResponse contract = scenario.createContract(data);
        String participant = scenario.requestParticipation(data, contract.identifier(), participants);

        contracts.changeParticipantStatus(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                        participant, null)
                .expectBadRequest();
    }

    @Test
    @DisplayName("VALIDATION - impede rejeitar novamente uma solicitação rejeitada")
    void impedeTransicaoDeStatusInvalida() {
        ContractResponse contract = scenario.createContract(data);
        String participant = scenario.requestParticipation(data, contract.identifier(), participants);
        contracts.changeParticipantStatus(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                        participant, new ParticipantFactory().reject()).expectOk();

        contracts.changeParticipantStatus(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                        participant, new ParticipantFactory().reject())
                .expectConflict();
    }

    @Test
    @DisplayName("QUERY - retorna não encontrado para participante inexistente")
    void alteraParticipanteInexistente() {
        ContractResponse contract = scenario.createContract(data);

        contracts.changeParticipantStatus(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                        "missing-participant", new ParticipantFactory().reject())
                .expectNotFound();
    }

    @Test
    @DisplayName("AUTH - exige permissão administrativa para alterar status")
    void exigePermissaoAdministrativa() {
        ContractResponse contract = scenario.createContract(data);
        String participant = scenario.requestParticipation(data, contract.identifier(), participants);
        authorization.forbidden();

        contracts.changeParticipantStatus(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                        participant, new ParticipantFactory().reject())
                .expectForbidden();
        authorization.verifyCalledWithPolicy("ADM");
    }

    @Test
    @DisplayName("UPDATE - aprova solicitação com configuração e pode revogá-la")
    void aprovaERevogaSolicitacao() {
        ResourceSharingScenario.EnvironmentMapping mapping = scenario.environmentMapping(data);
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
        String participant = scenario.requestParticipation(data, contract.identifier(), participants);

        contracts.changeParticipantStatus(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                        participant, new ParticipantFactory().approve(mapping.source(), mapping.destination()))
                .expectOk().expect("status", "APPROVED");
        contracts.changeParticipantStatus(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                        participant, new ParticipantFactory().revoke())
                .expectOk().expect("status", "REVOKED");
    }
}
