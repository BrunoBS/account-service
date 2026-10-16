package br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration.runtime.DynamicParameterErrorLogger;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.support.DynamicParameterRuntimeTestSupport;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.operations.runtime.DynamicParameterRuntimeService;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import io.restassured.http.ContentType;

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

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ParameterKeyRedisIT extends DynamicParameterRuntimeTestSupport {
    @MockitoSpyBean private RedisStore store;
    @Autowired private DynamicParameterRuntimeService runtime;
    @Autowired private PlatformTransactionManager transactions;

    @AfterEach
    void resetStore() {
        reset(store);
    }

    @Test
    void redisFailureDoesNotUndoPublicationAndHourlyJobRecoversIt() {
        String identifier = create("recoverable", "CONFIGURATION", "1");
        doThrow(new IllegalStateException("Forced Redis failure"))
                .when(store)
                .setAll(org.mockito.ArgumentMatchers.anyMap());
        request()
                .contentType(ContentType.JSON)
                .body(Map.of("identifiers", List.of(identifier)))
                .post(BASE + "/publish")
                .then()
                .statusCode(202);
        await(
                () ->
                        "PUBLISHED"
                                        .equals(
                                                jdbc.queryForObject(
                                                        "SELECT status_code FROM parameter_keys"
                                                                + " WHERE identifier=?",
                                                        String.class,
                                                        identifier))
                                && publishedAuditCount() == 1);
        // Wait for the worker to finish its Redis attempt before removing the failure.
        org.mockito.Mockito.verify(store, org.mockito.Mockito.timeout(5000))
                .setAll(org.mockito.ArgumentMatchers.anyMap());
        assertThat(runtimeValue("recoverable")).isNull();
        reset(store);
        runtime.reconcile();
        assertThat(runtimeValue("recoverable").get("value").intValue()).isEqualTo(1);
    }

    @Test
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
                .when(store)
                .setAll(
                        org.mockito.ArgumentMatchers.argThat(
                                batch -> batch.containsKey("account-service.failure")));
        Logger logger = (Logger) LoggerFactory.getLogger(DynamicParameterErrorLogger.class);
        ListAppender<ILoggingEvent> events = new ListAppender<>();
        events.start();
        logger.addAppender(events);
        try {
            runtime.reconcile();
            assertThat(runtimeValue("healthy").get("value").intValue()).isEqualTo(3);
            assertThat(runtimeValue("failure")).isNull();
            assertThat(runtimeValue("same-batch")).isNull();
            assertThat(events.list).hasSize(2);
            var event = events.list.getFirst();
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            String error = event.getMDCPropertyMap().get("platform.error");
            assertThat(error)
                    .contains(first, "account-service.failure", "publishedValue", "visible", "code")
                    .doesNotContain("do-not-log");
            assertThat(event.getThrowableProxy()).isNull();
        } finally {
            logger.detachAppender(events);
            events.stop();
        }
    }

    @Test
    void rolledBackPublicationNeverReachesRedis() {
        String identifier = create("rollback", "CONFIGURATION", "1");
        var template = new TransactionTemplate(transactions);
        template.executeWithoutResult(
                status -> {
                    jdbc.update(
                            "UPDATE parameter_keys SET"
                                + " published_value=value,published_type_code=type_code,status_code='PUBLISHED'"
                                + " WHERE identifier=?",
                            identifier);
                    status.setRollbackOnly();
                });
        runtime.reconcile();
        assertThat(runtimeValue("rollback")).isNull();
    }
}
