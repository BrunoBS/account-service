package br.com.portalmanager.platform.workspace.integration.feature.sharing.participant;

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

class ParticipantListAvailableContractsIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("QUERY - lista contratos disponíveis para a aplicação")
    void listaContratosDisponiveis() {
        var contract = scenario.createContract(data);
                participants.listContracts(data.participantWorkspace(), data.participantApplication(),
                                data.ownerWorkspace(), data.ownerApplication(), 0, 20)
                        .expectOk().expectContains("content[0].identifier", contract.identifier());
    }

    @Autowired private jakarta.persistence.EntityManagerFactory entityManagerFactory;

    @Test
    @DisplayName("REGRESSION FIN-HTTP-001 - valida limite do offset antes da persistência")
    void rejeitaOverflowDoOffsetESuportaMaiorPaginaRepresentavel() {
        int page = Integer.MAX_VALUE / 100;
        participants.listContracts(data.participantWorkspace(), data.participantApplication(), data.ownerWorkspace(), data.ownerApplication(), page, 100).expectOk().expectSize("content", 0);
        org.hibernate.stat.Statistics statistics = entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        boolean previous = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        try {
            statistics.clear();
            page = Integer.MAX_VALUE / 100 + 1;
            participants.listContracts(data.participantWorkspace(), data.participantApplication(), data.ownerWorkspace(), data.ownerApplication(), page, 100).expectBadRequest();
            page = Integer.MAX_VALUE;
            participants.listContracts(data.participantWorkspace(), data.participantApplication(), data.ownerWorkspace(), data.ownerApplication(), page, 100).expectBadRequest();
            org.assertj.core.api.Assertions.assertThat(statistics.getPrepareStatementCount()).isZero();
        } finally { statistics.setStatisticsEnabled(previous); }
    }
}
