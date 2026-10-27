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

class ContractUpdateIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("UPDATE - altera descrição do contrato")
    void atualizaDescricao() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
        contracts.update(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                                factory.withDescription(data.feature(), "Updated sharing contract"))
                        .expectOk().expect("description", "Updated sharing contract");
        contracts.find(data.ownerWorkspace(), data.ownerApplication(), contract.identifier())
                .expectOk().expect("description", "Updated sharing contract");
    }

    @Test
    @DisplayName("VALIDATION - rejeita descrição acima do limite")
    void rejeitaDescricaoAcimaDoLimite() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
                contracts.update(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                                factory.withDescriptionAboveMaximum(data.feature()))
                        .expectBadRequest();
    }

    @Test
    @DisplayName("VALIDATION - não permite alterar a feature do contrato")
    void naoPermiteAlterarFeature() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);

        contracts.update(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                        factory.withFeature("another-feature"))
                .expectConflict();
    }

    @Test
    @DisplayName("QUERY - retorna não encontrado ao atualizar contrato inexistente")
    void naoAtualizaContratoInexistente() {
        contracts.update(data.ownerWorkspace(), data.ownerApplication(), "missing-contract",
                        factory.valid(data.feature()))
                .expectNotFound();
    }

    @Test
    @DisplayName("VALIDATION - não permite atualizar contrato inativo")
    void naoAtualizaContratoInativo() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
        contracts.inactivate(data.ownerWorkspace(), data.ownerApplication(), contract.identifier()).expectOk();

        contracts.update(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                        factory.withDescription(data.feature(), "Updated"))
                .expectNotFound();
    }

    @Test
    @DisplayName("AUTH - exige permissão administrativa para atualizar contrato")
    void exigePermissaoAdministrativaParaAtualizar() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
        authorization.forbidden();

        contracts.update(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                        factory.withDescription(data.feature(), "Updated"))
                .expectForbidden();
        authorization.verifyCalledWithPolicy("ADM");
    }
}
