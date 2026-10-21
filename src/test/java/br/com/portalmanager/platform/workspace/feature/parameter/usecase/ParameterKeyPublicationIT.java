package br.com.portalmanager.platform.workspace.feature.parameter.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.ParameterPublicationStore;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.PublishedParameterValue;
import br.com.portalmanager.platform.workspace.feature.parameter.support.DynamicParameterRuntimeTestSupport;

import io.restassured.http.ContentType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ParameterKeyPublicationIT extends DynamicParameterRuntimeTestSupport {
    @MockitoSpyBean private ParameterPublicationStore store;

    @MockitoSpyBean
    private br.com.portalmanager.platform.library.audit.outbox.AuditOutboxAppender audit;

    @AfterEach
    void resetOperations() {
        reset(store, audit);
    }

    @Test
    void publishesOneWithAnEmptyAcceptedResponseAndPreservesTheSnapshotOnEdit() {
        String identifier = create("single", "CONFIGURATION", "{\"approved\":true}");
        publish(identifier);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status_code FROM parameter_keys WHERE identifier=?",
                                String.class,
                                identifier))
                .isEqualTo("PUBLISHED");
        assertThat(runtimeValue("single").get("value").get("approved").booleanValue()).isTrue();
        assertThat(redis.ttl("account-service.single")).isEmpty();
        request()
                .contentType(ContentType.JSON)
                .body(
                        Map.of(
                                "version",
                                1,
                                "type",
                                "CONFIGURATION",
                                "value",
                                Map.of("approved", false)))
                .put(BASE + "/" + identifier)
                .then()
                .statusCode(200);
        assertThat(runtimeValue("single").get("value").get("approved").booleanValue()).isTrue();
        assertThat(
                        mapper.readTree(
                                        jdbc.queryForObject(
                                                "SELECT published_value FROM parameter_keys WHERE"
                                                        + " identifier=?",
                                                String.class,
                                                identifier))
                                .get("approved")
                                .booleanValue())
                .isTrue();
        assertThat(publishedAuditCount()).isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM audit_outbox WHERE"
                                    + " JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.eventType'))='PARAMETER_KEY_PUBLISHED'"
                                    + " AND CAST(metadata AS CHAR) LIKE '%parameter-owner%'",
                                Integer.class))
                .isEqualTo(1);
    }

    @Test
    void processesEachIdentifierIndependentlyAndContinuesAfterAnInvalidItem() {
        String first = create("first", "CONFIGURATION", "1");
        String inactive = create("inactive", "CONFIGURATION", "2");
        String last = create("last", "CONFIGURATION", "3");
        request().patch(BASE + "/" + inactive + "/inactivate").then().statusCode(200);
        request()
                .contentType(ContentType.JSON)
                .body(
                        Map.of(
                                "identifiers",
                                List.of(first, UUID.randomUUID().toString(), inactive, last)))
                .post(BASE + "/publish")
                .then()
                .statusCode(202)
                .body(org.hamcrest.Matchers.emptyString());
        await(() -> runtimeValue("last") != null);
        assertThat(runtimeValue("first").get("value").intValue()).isEqualTo(1);
        assertThat(runtimeValue("last").get("value").intValue()).isEqualTo(3);
        assertThat(runtimeValue("inactive")).isNull();
        assertThat(publishedAuditCount()).isEqualTo(2);
    }

    @Test
    void returnsAcceptedBeforeTheBackgroundWorkCompletes() throws Exception {
        String identifier = create("async", "CONFIGURATION", "1");
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(
                        invocation -> {
                            entered.countDown();
                            if (!release.await(10, TimeUnit.SECONDS))
                                throw new AssertionError("Worker not released");
                            return invocation.callRealMethod();
                        })
                .when(store)
                .publish(org.mockito.ArgumentMatchers.anyMap());
        try {
            request()
                    .contentType(ContentType.JSON)
                    .body(Map.of("identifiers", List.of(identifier)))
                    .post(BASE + "/publish")
                    .then()
                    .statusCode(202)
                    .body(org.hamcrest.Matchers.emptyString());
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(runtimeValue("async")).isNull();
        } finally {
            release.countDown();
        }
        await(() -> runtimeValue("async") != null);
    }

    @Test
    void oneFailedTransactionDoesNotRollBackOtherItemsOrSendItsValueToRedis() {
        String failed = create("transaction-failed", "CONFIGURATION", "1");
        String successful = create("transaction-successful", "CONFIGURATION", "2");
        java.util.concurrent.atomic.AtomicBoolean first = new java.util.concurrent.atomic.AtomicBoolean(true);
        doAnswer(
                        invocation -> {
                            if (first.getAndSet(false))
                                throw new IllegalStateException("Forced audit failure");
                            return invocation.callRealMethod();
                        })
                .when(audit)
                .append(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        request()
                .contentType(ContentType.JSON)
                .body(Map.of("identifiers", List.of(failed, successful)))
                .post(BASE + "/publish")
                .then()
                .statusCode(202);
        await(() -> runtimeValue("transaction-successful") != null);
        assertThat(runtimeValue("transaction-failed")).isNull();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status_code FROM parameter_keys WHERE identifier=?",
                                String.class,
                                failed))
                .isEqualTo("DRAFT");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT published_value IS NULL FROM parameter_keys WHERE"
                                        + " identifier=?",
                                Boolean.class,
                                failed))
                .isTrue();
        assertThat(publishedAuditCount()).isEqualTo(1);
    }

    @Test
    void rejectsMalformedOrUnauthorizedRequestsBeforeAcceptingAndRemovesLegacyEndpoints() {
        String identifier = create("validation", "CONFIGURATION", "1");
        for (Object identifiers :
                List.of(List.of(), List.of(identifier, identifier), List.of("invalid"))) {
            request()
                    .contentType(ContentType.JSON)
                    .body(Map.of("identifiers", identifiers))
                    .post(BASE + "/publish")
                    .then()
                    .statusCode(400);
        }
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_DEV"));
        authorization.forbiddenResource(resource -> resource.header("policy", "OWNER"));
        request()
                .contentType(ContentType.JSON)
                .body(Map.of("identifiers", List.of(identifier)))
                .post(BASE + "/publish")
                .then()
                .statusCode(403);
        request().patch(BASE + "/" + identifier + "/publish").then().statusCode(404);
        request()
                .contentType(ContentType.JSON)
                .body(Map.of("identifiers", List.of(identifier)))
                .post(BASE + "/publish-batch")
                .then()
                .statusCode(405);
        assertThat(runtimeValue("validation")).isNull();
    }
}
