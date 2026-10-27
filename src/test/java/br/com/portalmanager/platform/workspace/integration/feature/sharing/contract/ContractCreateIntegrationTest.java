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

class ContractCreateIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("CREATE - cria contrato de compartilhamento")
    void criaContrato() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse created =
                contracts.create(data.ownerWorkspace(), data.ownerApplication(), factory.valid(data.feature()))
                        .expectCreated()
                        .extractBody();
        contracts.find(data.ownerWorkspace(), data.ownerApplication(), created.identifier())
                .expectOk()
                .expect("identifier", created.identifier())
                        .expect("featureIdentifier", data.feature())
                        .expect("lifecycle", "ACTIVE");
    }

    @Test
    @DisplayName("VALIDATION - exige feature no contrato")
    void rejeitaContratoSemFeature() {
        contracts.create(data.ownerWorkspace(), data.ownerApplication(), factory.withoutFeature())
                        .expectBadRequest();
    }

    @Test
    @DisplayName("VALIDATION - rejeita descrição acima do limite")
    void rejeitaDescricaoAcimaDoLimite() {
        contracts.create(data.ownerWorkspace(), data.ownerApplication(),
                        factory.withDescriptionAboveMaximum(data.feature()))
                .expectBadRequest();
    }

    @Test
    @DisplayName("VALIDATION - rejeita contrato duplicado para a mesma feature")
    void rejeitaContratoDuplicadoParaFeature() {
        scenario.createContract(data);

        contracts.create(data.ownerWorkspace(), data.ownerApplication(), factory.valid(data.feature()))
                .expectConflict();
    }

    @Test
    @DisplayName("VALIDATION - rejeita feature inexistente")
    void rejeitaFeatureInexistente() {
        contracts.create(data.ownerWorkspace(), data.ownerApplication(), factory.withFeature("missing-feature"))
                .expectNotFound();
    }

    @Test
    @DisplayName("AUTH - exige permissão administrativa para criar contrato")
    void exigePermissaoAdministrativaParaCriar() {
        authorization.forbidden();

        contracts.create(data.ownerWorkspace(), data.ownerApplication(), factory.valid(data.feature()))
                .expectForbidden();
        authorization.verifyCalledWithPolicy("ADM");
    }
}
