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

class ContractActivateIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("UPDATE - ativa contrato inativo")
    void ativaContratoInativo() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
                contracts.inactivate(data.ownerWorkspace(), data.ownerApplication(), contract.identifier())
                        .expectOk();
                contracts.activate(data.ownerWorkspace(), data.ownerApplication(), contract.identifier())
                        .expectOk().expect("lifecycle", "ACTIVE");
    }

    @Test
    @DisplayName("VALIDATION - rejeita ativar contrato já ativo")
    void rejeitaAtivarContratoAtivo() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);

        contracts.activate(data.ownerWorkspace(), data.ownerApplication(), contract.identifier())
                .expectConflict();
    }

    @Test
    @DisplayName("QUERY - retorna não encontrado ao ativar contrato inexistente")
    void naoAtivaContratoInexistente() {
        contracts.activate(data.ownerWorkspace(), data.ownerApplication(), "missing-contract")
                .expectNotFound();
    }

    @Test
    @DisplayName("AUTH - exige permissão administrativa para ativar contrato")
    void exigePermissaoAdministrativaParaAtivar() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
        contracts.inactivate(data.ownerWorkspace(), data.ownerApplication(), contract.identifier()).expectOk();
        authorization.forbidden();

        contracts.activate(data.ownerWorkspace(), data.ownerApplication(), contract.identifier())
                .expectForbidden();
        authorization.verifyCalledWithPolicy("ADM");
    }
}
