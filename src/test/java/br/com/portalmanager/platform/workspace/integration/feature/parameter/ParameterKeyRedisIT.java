package br.com.portalmanager.platform.workspace.integration.feature.parameter;

import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.ParameterPublicationStore;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.integration.feature.parameter.support.DynamicParameterRuntimeTestSupport;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.operations.ParameterKeyPublisherService;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("Integração HTTP - Parameter Key Redis")
class ParameterKeyRedisIT extends DynamicParameterRuntimeTestSupport {
    @Autowired private ParameterKeyPublisherService publisher;
    @Autowired private PlatformTransactionManager transactions;

    @AfterEach
    void resetStore() {
        reset(publicationStore);
    }

    @Test

    @DisplayName("RESTORE - redis failure does not undo publication and hourly job recovers it")
    void redisFailureDoesNotUndoPublicationAndHourlyJobRecoversIt() {
        String identifier = create("recoverable", "CONFIGURATION", "1");
        doThrow(new IllegalStateException("Forced Redis failure"))
                .when(publicationStore)
                .publish(org.mockito.ArgumentMatchers.anyMap());
        publishBatch(List.of(identifier)).expectAcceptedWithEmptyBody();
        await(
                () ->
                        "PUBLISHED".equals(parameterKeys.findProjection(identifier)
                                        .map(projection -> projection.getStatus()).orElse(null))
                                && publishedAuditCount() == 1);
        // Wait for the worker to finish its Redis attempt before removing the failure.
        org.mockito.Mockito.verify(publicationStore, org.mockito.Mockito.timeout(5000))
                .publish(org.mockito.ArgumentMatchers.anyMap());
        assertThat(runtimeValue("recoverable")).isNull();
        reset(publicationStore);
        publisher.reconcile();
        assertThat(runtimeValue("recoverable").get("value").intValue()).isEqualTo(1);
    }

    @Test

    @DisplayName("SMOKE - scheduler continues after abatch failure and logs every affected snapshot")
    void schedulerContinuesAfterABatchFailureAndLogsEveryAffectedSnapshot() {
        String first =
                create(
                        "failure",
                        "CONFIGURATION",
                        "{\"password\":\"do-not-log\",\"visible\":true}");
        publish(first);
        String second = create("same-batch", "CONFIGURATION", "2");
        publish(second);
        String third = create("healthy", "CONFIGURATION", "3");
        publish(third);
        redis.delete("account-service.failure");
        redis.delete("account-service.same-batch");
        redis.delete("account-service.healthy");
        doThrow(new IllegalStateException("do-not-log"))
                .when(publicationStore)
                .publish(
                        org.mockito.ArgumentMatchers.argThat(
                                batch -> batch.containsKey("account-service.failure")));
        Logger logger = (Logger) LoggerFactory.getLogger(ParameterKeyPublisherService.class);
        ListAppender<ILoggingEvent> events = new ListAppender<>();
        events.start();
        logger.addAppender(events);
        try {
            publisher.reconcile();
            assertThat(runtimeValue("healthy").get("value").intValue()).isEqualTo(3);
            assertThat(runtimeValue("failure")).isNull();
            assertThat(runtimeValue("same-batch")).isNull();
            assertThat(events.list).hasSize(2);
            assertThat(events.list).extracting(ILoggingEvent::getFormattedMessage)
                    .anySatisfy(message -> assertThat(message).contains(first))
                    .anySatisfy(message -> assertThat(message).contains(second));
            assertThat(events.list).allSatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.ERROR);
                assertThat(event.getFormattedMessage())
                        .contains("errorType=java.lang.IllegalStateException")
                        .doesNotContain("account-service.", "publishedValue", "visible", "do-not-log");
                assertThat(event.getThrowableProxy()).isNull();
                assertThat(event.getArgumentArray()).allSatisfy(argument -> {
                    assertThat(argument).isNotInstanceOf(Throwable.class);
                    assertThat(String.valueOf(argument)).doesNotContain("do-not-log", "visible");
                });
            });
        } finally {
            logger.detachAppender(events);
            events.stop();
        }
    }

    @Test

    @DisplayName("SMOKE - rolled back publication never reaches redis")
    void rolledBackPublicationNeverReachesRedis() {
        String identifier = create("rollback", "CONFIGURATION", "1");
        TransactionTemplate template = new TransactionTemplate(transactions);
        template.executeWithoutResult(
                status -> {
                    var parameter = parameterKeys.findForUpdate(identifier).orElseThrow();
                    parameter.publish(LocalDateTime.now());
                    parameterKeys.saveAndFlush(parameter);
                    status.setRollbackOnly();
                });
        publisher.reconcile();
        assertThat(runtimeValue("rollback")).isNull();
    }
}
