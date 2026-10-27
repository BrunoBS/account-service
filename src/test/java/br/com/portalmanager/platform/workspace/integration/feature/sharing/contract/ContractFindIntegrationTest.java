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

class ContractFindIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("QUERY - busca contrato pelo identificador")
    void buscaContratoExistente() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
                contracts.find(data.ownerWorkspace(), data.ownerApplication(), contract.identifier())
                        .expectOk().expect("identifier", contract.identifier());
    }

    @Test
    @DisplayName("QUERY - retorna não encontrado para contrato inexistente")
    void naoEncontraContratoAusente() {
        contracts.find(data.ownerWorkspace(), data.ownerApplication(), "missing-contract")
                        .expectNotFound();
    }

    @Test
    @DisplayName("AUTH - exige permissão de leitura para consultar contrato")
    void exigePermissaoDeLeitura() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
        authorization.forbidden();

        contracts.find(data.ownerWorkspace(), data.ownerApplication(), contract.identifier())
                .expectForbidden();
        authorization.verifyCalledWithPolicy("DEV");
    }

    @Test
    @DisplayName("QUERY - contrato de outra aplicação não fica visível")
    void naoExpõeContratoDeOutraAplicacao() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);

        contracts.find(data.participantWorkspace(), data.participantApplication(), contract.identifier())
                .expectNotFound();
    }
}
