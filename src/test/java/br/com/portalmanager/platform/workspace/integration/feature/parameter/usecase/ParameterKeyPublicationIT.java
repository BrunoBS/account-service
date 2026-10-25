package br.com.portalmanager.platform.workspace.integration.feature.parameter.usecase;

import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;

import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.ParameterPublicationStore;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.PublishedParameterValue;
import br.com.portalmanager.platform.workspace.integration.feature.parameter.support.DynamicParameterRuntimeTestSupport;


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

@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("Integração HTTP - Parameter Key Publication")
class ParameterKeyPublicationIT extends DynamicParameterRuntimeTestSupport {


    @AfterEach
    void resetOperations() {
        reset(publicationStore, auditAppender);
    }

    @Test

    @DisplayName("UPDATE - publishes one with an empty accepted response and preserves the snapshot on edit")
    void publishesOneWithAnEmptyAcceptedResponseAndPreservesTheSnapshotOnEdit() {
        String identifier = create("single", "CONFIGURATION", "{\"approved\":true}");
        publish(identifier);
        assertThat(parameterKeys.findProjection(identifier).orElseThrow().getStatus())
                .isEqualTo("PUBLISHED");
        assertThat(runtimeValue("single").get("value").get("approved").booleanValue()).isTrue();
        assertThat(redis.ttl("account-service.single")).isEmpty();
        updateParameter(identifier,
                Map.of("version", 1, "type", "CONFIGURATION", "value", Map.of("approved", false)))
                .expectOk();
        assertThat(runtimeValue("single").get("value").get("approved").booleanValue()).isTrue();
        assertThat(parameterKeys.findRuntimeSnapshot(identifier).orElseThrow()
                        .getValue().get("approved").booleanValue())
                .isTrue();
        assertThat(publishedAuditCount("parameter-owner")).isEqualTo(1);
    }

    @Test

    @DisplayName("VALIDATION - processes each identifier independently and continues after an invalid item")
    void processesEachIdentifierIndependentlyAndContinuesAfterAnInvalidItem() {
        String first = create("first", "CONFIGURATION", "1");
        String inactive = create("inactive", "CONFIGURATION", "2");
        String last = create("last", "CONFIGURATION", "3");
        inactivateParameter(inactive).expectOk();
        publishBatch(List.of(first, UUID.randomUUID().toString(), inactive, last))
                .expectAcceptedWithEmptyBody();
        await(() -> runtimeValue("last") != null);
        assertThat(runtimeValue("first").get("value").intValue()).isEqualTo(1);
        assertThat(runtimeValue("last").get("value").intValue()).isEqualTo(3);
        assertThat(runtimeValue("inactive")).isNull();
        assertThat(publishedAuditCount()).isEqualTo(2);
    }

    @Test

    @DisplayName("QUERY - returns accepted before the background work completes")
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
                .when(publicationStore)
                .publish(org.mockito.ArgumentMatchers.anyMap());
        try {
            publishBatch(List.of(identifier)).expectAcceptedWithEmptyBody();
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(runtimeValue("async")).isNull();
        } finally {
            release.countDown();
        }
        await(() -> runtimeValue("async") != null);
    }

    @Test

    @DisplayName("SMOKE - one failed transaction does not roll back other items or send its value to redis")
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
                .when(auditAppender)
                .append(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        publishBatch(List.of(failed, successful)).expectAcceptedWithEmptyBody();
        await(() -> runtimeValue("transaction-successful") != null);
        assertThat(runtimeValue("transaction-failed")).isNull();
        assertThat(parameterKeys.findProjection(failed).orElseThrow().getStatus()).isEqualTo("DRAFT");
        assertThat(parameterKeys.findRuntimeSnapshot(failed).orElseThrow().getValue()).isNull();
        assertThat(publishedAuditCount()).isEqualTo(1);
    }

    @Test

    @DisplayName("AUTH - rejects malformed or unauthorized requests before accepting and removes legacy endpoints")
    void rejectsMalformedOrUnauthorizedRequestsBeforeAcceptingAndRemovesLegacyEndpoints() {
        String identifier = create("validation", "CONFIGURATION", "1");
        for (Object identifiers :
                List.of(List.of(), List.of(identifier, identifier), List.of("invalid"))) {
            publishBatch(identifiers).expectBadRequest();
        }
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_DEV"));
        authorization.forbiddenResource(resource -> resource.header("policy", "OWNER"));
        publishBatch(List.of(identifier)).expectForbidden();
        publishRemovedSingleEndpoint(identifier).expectNotFound();
        publishRemovedBatchEndpoint(List.of(identifier)).expectMethodNotAllowed();
        assertThat(runtimeValue("validation")).isNull();
    }
}
