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

class ContractDeleteIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("DELETE - remove contrato inativo")
    void removeContratoInativo() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
                contracts.inactivate(data.ownerWorkspace(), data.ownerApplication(), contract.identifier())
                        .expectOk();
                contracts.delete(data.ownerWorkspace(), data.ownerApplication(), contract.identifier())
                        .expectNoContent();
                contracts.find(data.ownerWorkspace(), data.ownerApplication(), contract.identifier())
                        .expectNotFound();
    }

    @Test
    @DisplayName("VALIDATION - impede remover contrato ativo")
    void impedeRemoverContratoAtivo() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);

        contracts.delete(data.ownerWorkspace(), data.ownerApplication(), contract.identifier())
                .expectConflict();
        contracts.find(data.ownerWorkspace(), data.ownerApplication(), contract.identifier()).expectOk();
    }

    @Test
    @DisplayName("QUERY - retorna não encontrado ao remover contrato inexistente")
    void naoRemoveContratoInexistente() {
        contracts.delete(data.ownerWorkspace(), data.ownerApplication(), "missing-contract")
                .expectNotFound();
    }

    @Test
    @DisplayName("AUTH - exige permissão administrativa para remover contrato")
    void exigePermissaoAdministrativaParaRemover() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
        contracts.inactivate(data.ownerWorkspace(), data.ownerApplication(), contract.identifier()).expectOk();
        authorization.forbidden();

        contracts.delete(data.ownerWorkspace(), data.ownerApplication(), contract.identifier())
                .expectForbidden();
        authorization.verifyCalledWithPolicy("ADM");
    }

    @Test
    @DisplayName("DELETE - remove contrato inativo e suas participações")
    void removeContratoEParticipacoesAssociadas() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
        String participant = scenario.requestParticipation(data, contract.identifier(), participants);
        contracts.inactivate(data.ownerWorkspace(), data.ownerApplication(), contract.identifier()).expectOk();

        contracts.delete(data.ownerWorkspace(), data.ownerApplication(), contract.identifier()).expectNoContent();
        participants.find(data.participantWorkspace(), data.participantApplication(), participant)
                .expectNotFound();
    }
}
