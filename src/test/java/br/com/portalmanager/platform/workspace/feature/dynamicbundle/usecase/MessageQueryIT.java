package br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.message.MessageOutput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.message.MessageQueryService;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

class MessageQueryIT  extends br.com.portalmanager.platform.workspace.support.WorkspaceIntegrationTestSupport{

    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private MessageQueryService query;

    @Test
    void loadsPageAndCountAndResolvesAllMicroserviceIdentifiersInOneBatch() {
        SchemaDefaultFixture.seed(jdbc);
        for (int index = 1; index <= 3; index++) {
            String identifier = "11111111-1111-1111-1111-11111111111" + index;
            jdbc.update(
                    "INSERT INTO service_context_microservices"
                        + " (identifier,code,name,settings,lifecycle_code,created_at,updated_at)"
                        + " VALUES (?,?,?,'{}','ACTIVE',NOW(6),NOW(6))",
                    identifier,
                    "review-service-" + index,
                    "Review service " + index);
            jdbc.update(
                    "INSERT INTO messages"
                        + " (identifier,microservice_id,message_key,code,http_status,lifecycle_code,created_at,updated_at)"
                        + " SELECT UUID(),id,?,'REVIEW-101',400,'ACTIVE',NOW(6),NOW(6) FROM"
                        + " service_context_microservices WHERE identifier=?",
                    "review.query." + index,
                    identifier);
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
                            "11111111-1111-1111-1111-111111111111",
                            "11111111-1111-1111-1111-111111111112");
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
        } finally {
            statistics.setStatisticsEnabled(statisticsEnabled);
        }
    }
}
