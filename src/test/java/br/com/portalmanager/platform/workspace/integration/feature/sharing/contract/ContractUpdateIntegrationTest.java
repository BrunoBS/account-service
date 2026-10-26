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

class ContractUpdateIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("UPDATE - altera descrição do contrato")
    void atualizaDescricao() {
        var contract = scenario.createContract(data);
                contracts.update(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                                factory.withDescription(data.feature(), "Updated sharing contract"))
                        .expectOk().expect("description", "Updated sharing contract");
    }

    @Test
    @DisplayName("VALIDATION - rejeita descrição acima do limite")
    void rejeitaDescricaoAcimaDoLimite() {
        var contract = scenario.createContract(data);
                contracts.update(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                                factory.withDescriptionAboveMaximum(data.feature()))
                        .expectBadRequest();
    }
}
