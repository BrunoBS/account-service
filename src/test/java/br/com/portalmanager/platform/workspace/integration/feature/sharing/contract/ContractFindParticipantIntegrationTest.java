package br.com.portalmanager.platform.workspace.integration.feature.sharing.contract;

import br.com.portalmanager.platform.workspace.integration.feature.sharing.support.ContractClient;
import br.com.portalmanager.platform.workspace.integration.feature.sharing.support.ContractFactory;
import br.com.portalmanager.platform.workspace.integration.feature.sharing.support.ParticipantClient;
import br.com.portalmanager.platform.workspace.integration.feature.sharing.support.ParticipantFactory;
import br.com.portalmanager.platform.workspace.integration.feature.sharing.support.ResourceSharingScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ContractFindParticipantIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("QUERY - busca participante pelo contrato")
    void buscaParticipanteDoContrato() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
                String participant = scenario.requestParticipation(data, contract.identifier(), participants);
                contracts.findParticipant(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(), participant)
                        .expectOk().expect("status", "PENDING");
    }

    @Test
    @DisplayName("QUERY - retorna não encontrado para participante inexistente")
    void naoEncontraParticipanteAusente() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);

        contracts.findParticipant(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                        "missing-participant")
                .expectNotFound();
    }

    @Test
    @DisplayName("QUERY - não expõe participante de outro contrato")
    void naoExpõeParticipanteDeOutroContrato() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse firstContract = scenario.createContract(data);
        String participant = scenario.requestParticipation(data, firstContract.identifier(), participants);
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse anotherContract = contracts.create(data.ownerWorkspace(), data.ownerApplication(),
                new ContractFactory().valid(scenario.createAnotherShareableFeature()))
                .expectCreated().extractBody();

        contracts.findParticipant(data.ownerWorkspace(), data.ownerApplication(), anotherContract.identifier(),
                        participant)
                .expectNotFound();
    }

    @Test
    @DisplayName("AUTH - exige permissão de leitura para consultar participante")
    void exigePermissaoDeLeitura() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
        String participant = scenario.requestParticipation(data, contract.identifier(), participants);
        authorization.forbidden();

        contracts.findParticipant(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(), participant)
                .expectForbidden();
        authorization.verifyCalledWithPolicy("DEV");
    }
}
