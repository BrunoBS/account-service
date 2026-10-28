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

class ContractListIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("QUERY - lista contratos ativos da aplicação proprietária")
    void listaContratosAtivos() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
                contracts.list(data.ownerWorkspace(), data.ownerApplication(), "ACTIVE", 0, 20)
                        .expectOk().expectContains("content[0].identifier", contract.identifier());
    }

    @Test
    @DisplayName("VALIDATION - rejeita número de página inválido")
    void rejeitaPaginaInvalida() {
        contracts.list(data.ownerWorkspace(), data.ownerApplication(), null, -1, 20)
                        .expectBadRequest();
    }

    @Test
    @DisplayName("QUERY - filtra contratos inativos")
    void filtraContratosInativos() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
        contracts.inactivate(data.ownerWorkspace(), data.ownerApplication(), contract.identifier()).expectOk();

        contracts.list(data.ownerWorkspace(), data.ownerApplication(), "INACTIVE", 0, 20)
                .expectOk()
                .expectSize("content", 1)
                .expect("content[0].identifier", contract.identifier());
    }

    @Test
    @DisplayName("VALIDATION - rejeita lifecycle desconhecido")
    void rejeitaLifecycleDesconhecido() {
        contracts.list(data.ownerWorkspace(), data.ownerApplication(), "UNKNOWN", 0, 20)
                .expectBadRequest();
    }

    @Test
    @DisplayName("VALIDATION - rejeita tamanho de página fora do limite")
    void rejeitaTamanhoDePaginaAcimaDoLimite() {
        contracts.list(data.ownerWorkspace(), data.ownerApplication(), null, 0, 101)
                .expectBadRequest();
    }

    @Test
    @DisplayName("AUTH - exige permissão de leitura para listar contratos")
    void exigePermissaoDeLeitura() {
        authorization.forbidden();

        contracts.list(data.ownerWorkspace(), data.ownerApplication(), null, 0, 20)
                .expectForbidden();
        authorization.verifyCalledWithPolicy("DEV");
    }

    @Autowired private jakarta.persistence.EntityManagerFactory entityManagerFactory;

    @Test
    @DisplayName("REGRESSION FIN-HTTP-001 - valida limite do offset antes da persistência")
    void rejeitaOverflowDoOffsetESuportaMaiorPaginaRepresentavel() {
        int page = Integer.MAX_VALUE / 100;
        contracts.list(data.ownerWorkspace(), data.ownerApplication(), null, page, 100).expectOk().expectSize("content", 0);
        org.hibernate.stat.Statistics statistics = entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        boolean previous = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        try {
            statistics.clear();
            page = Integer.MAX_VALUE / 100 + 1;
            contracts.list(data.ownerWorkspace(), data.ownerApplication(), null, page, 100).expectBadRequest();
            page = Integer.MAX_VALUE;
            contracts.list(data.ownerWorkspace(), data.ownerApplication(), null, page, 100).expectBadRequest();
            org.assertj.core.api.Assertions.assertThat(statistics.getPrepareStatementCount()).isZero();
        } finally { statistics.setStatisticsEnabled(previous); }
    }
}
