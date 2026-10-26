package br.com.portalmanager.platform.workspace.integration.feature.sharing.contract;

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

class ContractCreateIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("CREATE - cria contrato de compartilhamento")
    void criaContrato() {
        contracts.create(data.ownerWorkspace(), data.ownerApplication(), factory.valid(data.feature()))
                        .expectCreated()
                        .expect("featureIdentifier", data.feature())
                        .expect("lifecycle", "ACTIVE");
    }

    @Test
    @DisplayName("VALIDATION - exige feature no contrato")
    void rejeitaContratoSemFeature() {
        contracts.create(data.ownerWorkspace(), data.ownerApplication(), factory.withoutFeature())
                        .expectBadRequest();
    }
}
