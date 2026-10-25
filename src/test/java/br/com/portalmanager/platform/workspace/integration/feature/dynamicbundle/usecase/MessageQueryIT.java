package br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.message.MessageOutput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.message.MessageQueryService;
import br.com.portalmanager.platform.workspace.integration.support.SchemaDefaultScenario;
import br.com.portalmanager.platform.workspace.integration.feature.parameter.support.MicroserviceClient;
import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message.support.MessageClient;
import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message.support.MessageFactory;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import tools.jackson.databind.json.JsonMapper;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

class MessageQueryIT  extends br.com.portalmanager.platform.workspace.integration.support.WorkspaceIntegrationTestSupport{

    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private MessageQueryService query;
    @Autowired private SchemaDefaultScenario schemaDefaults;
    @Autowired private PlatformRequestSpecificationFactory requests;
    @Autowired private MessageClient messages;
    private final MessageFactory messageFactory = new MessageFactory();

    @Test
    void loadsPageAndCountAndResolvesAllMicroserviceIdentifiersInOneBatch() {
        schemaDefaults.setup();
        var microservices = new MicroserviceClient(requests, JsonMapper.builder().build());
        java.util.List<String> identifiers = new java.util.ArrayList<>();
        for (int index = 1; index <= 3; index++) {
            String parent = microservices.create("review-service-" + index, "Review service " + index)
                    .expectCreated().identifier();
            identifiers.add(parent);
            messages.create(messageFactory.valid(parent, "review.query." + index, "REVIEW-10" + index))
                    .expectCreated();
        }
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean statisticsEnabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            Page<MessageOutput> page = query.findAll(null, null, null, null, PageRequest.of(0, 2));
            assertThat(page.getTotalElements()).isEqualTo(3);
            assertThat(page.getContent())
                    .extracting(MessageOutput::microserviceIdentifier)
                    .containsExactly(
                            identifiers.get(0), identifiers.get(1));
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
        } finally {
            statistics.setStatisticsEnabled(statisticsEnabled);
        }
    }
}
