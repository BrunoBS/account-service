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

class ContractParticipantsIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("QUERY - lista solicitações de participação do contrato")
    void listaParticipantesDoContrato() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
                scenario.requestParticipation(data, contract.identifier(), participants);
                contracts.participants(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                                "PENDING", 0, 20)
                        .expectOk().expectSize("content", 1);
    }

    @Test
    @DisplayName("QUERY - filtra participantes por aplicação e status")
    void filtraParticipantesPorAplicacaoEStatus() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
        scenario.requestParticipation(data, contract.identifier(), participants);

        contracts.participants(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                        null, data.participantApplication(), "PENDING", 0, 20)
                .expectOk()
                .expectSize("content", 1)
                .expect("content[0].participantApplicationIdentifier", data.participantApplication());
    }

    @Test
    @DisplayName("QUERY - retorna lista vazia quando o filtro de participante não corresponde")
    void retornaListaVaziaParaNomeSemCorrespondencia() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
        scenario.requestParticipation(data, contract.identifier(), participants);

        contracts.participants(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                        "no-such-application", null, null, 0, 20)
                .expectOk()
                .expectSize("content", 0);
    }

    @Test
    @DisplayName("VALIDATION - rejeita status de participante desconhecido")
    void rejeitaStatusDesconhecido() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);

        contracts.participants(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                        null, null, "UNKNOWN", 0, 20)
                .expectBadRequest();
    }

    @Test
    @DisplayName("QUERY - retorna não encontrado para contrato inexistente")
    void listaParticipantesDeContratoInexistente() {
        contracts.participants(data.ownerWorkspace(), data.ownerApplication(), "missing-contract",
                        null, null, null, 0, 20)
                .expectNotFound();
    }

    @Test
    @DisplayName("AUTH - exige permissão de leitura para listar participantes")
    void exigePermissaoDeLeitura() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
        authorization.forbidden();

        contracts.participants(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                        null, null, null, 0, 20)
                .expectForbidden();
        authorization.verifyCalledWithPolicy("DEV");
    }
}
