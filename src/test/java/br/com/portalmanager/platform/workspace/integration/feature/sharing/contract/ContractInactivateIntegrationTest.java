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

class ContractInactivateIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("UPDATE - inativa contrato ativo")
    void inativaContratoAtivo() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
                contracts.inactivate(data.ownerWorkspace(), data.ownerApplication(), contract.identifier())
                        .expectOk().expect("lifecycle", "INACTIVE");
    }

    @Test
    @DisplayName("VALIDATION - não inativa contrato que já está inativo")
    void rejeitaInativarContratoInativo() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
        contracts.inactivate(data.ownerWorkspace(), data.ownerApplication(), contract.identifier()).expectOk();

        contracts.inactivate(data.ownerWorkspace(), data.ownerApplication(), contract.identifier())
                .expectNotFound();
    }

    @Test
    @DisplayName("QUERY - retorna não encontrado ao inativar contrato inexistente")
    void naoInativaContratoInexistente() {
        contracts.inactivate(data.ownerWorkspace(), data.ownerApplication(), "missing-contract")
                .expectNotFound();
    }

    @Test
    @DisplayName("AUTH - exige permissão administrativa para inativar contrato")
    void exigePermissaoAdministrativaParaInativar() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
        authorization.forbidden();

        contracts.inactivate(data.ownerWorkspace(), data.ownerApplication(), contract.identifier())
                .expectForbidden();
        authorization.verifyCalledWithPolicy("ADM");
    }
}
