package br.com.portalmanager.platform.workspace.integration.feature.parameter.key;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.redis.annotation.RedisNamespace;
import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisProperties.NamespaceMode;
import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisProperties;
import br.com.portalmanager.platform.library.redis.internal.DefaultRedisStore;
import br.com.portalmanager.platform.library.redis.internal.RedisAccess;
import br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec;
import br.com.portalmanager.platform.library.testing.lifecycle.PlatformAwait;
import br.com.portalmanager.platform.library.testing.redis.annotation.WithRedis;
import br.com.portalmanager.platform.workspace.entrypoint.parameter.request.PublishParameterKeysRequest;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.projection.ParameterKeyRuntimeProjection;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.PublishedParameterValue;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.operations.ParameterKeyPublisherService;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.operations.ParameterKeyPublicationCommandService;
import br.com.portalmanager.platform.workspace.foundation.audit.repository.WorkspaceAuditOutboxRepository;
import br.com.portalmanager.platform.workspace.integration.feature.parameter.support.DynamicParameterClient;
import br.com.portalmanager.platform.workspace.integration.feature.parameter.support.DynamicParameterRequestFactory;
import br.com.portalmanager.platform.workspace.integration.feature.parameter.support.DynamicParameterScenario;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceFactory;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@WithRedis
class ParameterKeyReconciliationIntegrationTest extends BaseIntegrationTest {
    @Autowired private DynamicParameterClient client;
    @Autowired private DynamicParameterScenario scenario;
    @Autowired private MicroserviceClient microservices;
    @Autowired private ParameterKeyPublisherService publisher;
    @Autowired private ParameterKeyPublicationCommandService commands;
    @Autowired private WorkspaceAuditOutboxRepository outbox;
    @Autowired private ObjectMapper json;
    @Autowired private jakarta.persistence.EntityManagerFactory entityManagerFactory;
    @Autowired @RedisNamespace(value = "dynamic-parameter", mode = NamespaceMode.SHARED) private RedisStore redis;
    private final DynamicParameterRequestFactory factory = new DynamicParameterRequestFactory();
    private String parent;

    @BeforeEach void prepare() { parent = scenario.setup(); }

    private String create(String key) {
        return client.create(factory.valid(parent, key, "CONFIGURATION", "{\"value\":1}"))
                .expectCreated().identifier();
    }
    private void await(java.util.function.BooleanSupplier condition) {
        PlatformAwait.until(condition, Duration.ofSeconds(10), Duration.ofMillis(25));
    }
    private JsonNode value(String key) { return redis.get("account-service." + key, JsonNode.class).orElse(null); }
    private void publish(String identifier, String key) {
        client.publish(List.of(UUID.fromString(identifier)), List.of(UUID.fromString(parent))).expectAcceptedWithEmptyBody();
        await(() -> value(key) != null);
    }
    private long version(String identifier) { return client.findByIdentifier(identifier).expectOk().extractBody().version(); }

    @Test void lifecyclePreservesEditableDataAndPublishedSnapshotUntilExplicitPublication() {
        String id = create("lifecycle-flow");
        assertThat(version(id)).isZero();
        assertThat(parameterKeyRepository.findRuntimeSnapshot(id).orElseThrow().getValue()).isNull();
        assertThat(value("lifecycle-flow")).isNull();
        publish(id, "lifecycle-flow");
        assertThat(version(id)).isEqualTo(1);
        client.update(id, factory.update(1, "Description only", "CONFIGURATION", "{\"value\":1}"))
                .expectOk().expect("status", "PUBLISHED");
        client.update(id, factory.update(2, "Edited content", "CONFIGURATION", "{\"value\":2}"))
                .expectOk().expect("status", "DRAFT");
        assertThat(value("lifecycle-flow").path("value").path("value").asInt()).isEqualTo(1);
        long before = version(id);
        redis.delete("account-service.lifecycle-flow");
        publisher.reconcile();
        assertThat(value("lifecycle-flow").path("value").path("value").asInt()).isEqualTo(1);
        assertThat(version(id)).isEqualTo(before);
        client.inactivate(id).expectOk().expect("active", false).expect("status", "DRAFT");
        assertThat(value("lifecycle-flow")).isNull();
        ParameterKeyRuntimeProjection inactive = parameterKeyRepository.findRuntimeSnapshot(id).orElseThrow();
        assertThat(inactive.getValue()).isNull();
        assertThat(inactive.getType()).isNull();
        client.activate(id).expectOk().expect("active", true).expect("status", "DRAFT");
        assertThat(parameterKeyRepository.findRuntimeSnapshot(id).orElseThrow().getValue()).isNull();
        assertThat(client.findByIdentifier(id).expectOk().extractBody().value().path("value").asInt()).isEqualTo(2);
        assertThat(value("lifecycle-flow")).isNull();
    }

    @Test void schedulerDeletesAllInactiveKeysInBatchesBeforeSettingPublishedSnapshotsWithoutUpdatingMysql() {
        List<String> inactive = List.of(create("inactive-a"), create("inactive-b"), create("inactive-c"));
        for (String id : inactive) client.inactivate(id).expectOk();
        for (String key : List.of("inactive-a", "inactive-b", "inactive-c")) redis.set("account-service." + key, "orphan");
        String active = create("active-snapshot");
        publish(active, "active-snapshot");
        redis.delete("account-service.active-snapshot");
        Map<String, Long> versions = new java.util.LinkedHashMap<>();
        for (String id : inactive) versions.put(id, version(id));
        versions.put(active, version(active));
        clearInvocations(publicationStore, parameterKeyRepository);
        publisher.reconcile();
        org.mockito.InOrder order = inOrder(publicationStore);
        order.verify(publicationStore).removeAll(argThat(keys -> keys.size() == 2));
        order.verify(publicationStore).removeAll(argThat(keys -> keys.size() == 1));
        order.verify(publicationStore).publish(anyMap());
        assertThat(value("inactive-a")).isNull();
        assertThat(value("inactive-b")).isNull();
        assertThat(value("inactive-c")).isNull();
        assertThat(value("active-snapshot")).isNotNull();
        versions.forEach((id, previous) -> assertThat(version(id)).isEqualTo(previous));
        verify(parameterKeyRepository, never()).publishDrafts(anyCollection(), anyCollection(), anyString(), anyString(), any());
    }

    @Test void allIsRestrictedToRequestedMicroservicesAndMixedGroupsHaveDifferentEffects() {
        String draft = create("all-draft");
        String published = create("all-published");
        publish(published, "all-published");
        long previous = version(published);
        String inactive = create("all-inactive");
        client.inactivate(inactive).expectOk();
        redis.set("account-service.all-inactive", "orphan");
        String other = microservices.create(new MicroserviceFactory().valid("other-service", "Other"))
                .expectCreated().extractBody().identifier();
        String excluded = client.create(factory.valid(other, "excluded", "CONFIGURATION", "1")).expectCreated().identifier();
        clearInvocations(parameterKeyRepository);
        client.publishRequest(new PublishParameterKeysRequest(List.of(UUID.fromString(excluded)), true,
                List.of(UUID.fromString(parent)))).expectAcceptedWithEmptyBody();
        await(() -> value("all-draft") != null && value("all-inactive") == null);
        assertThat(version(draft)).isEqualTo(1);
        assertThat(version(published)).isEqualTo(previous);
        assertThat(version(excluded)).isZero();
        assertThat(redis.exists("other-service.excluded")).isFalse();
        redis.delete("account-service.all-published");
        client.publishRequest(Map.of("all", true, "microserviceIdentifiers", List.of(parent)))
                .expectAcceptedWithEmptyBody();
        await(() -> value("all-published") != null);
        redis.delete("account-service.all-published");
        client.publishRequest(new PublishParameterKeysRequest(null, true, List.of(UUID.fromString(parent))))
                .expectAcceptedWithEmptyBody();
        await(() -> value("all-published") != null);

        verify(parameterKeyRepository, atLeastOnce()).publishDrafts(anyCollection(), anyCollection(), eq("DRAFT"), eq("PUBLISHED"), any());
        assertThat(outbox.findAll().stream().filter(event -> "PARAMETER_KEY_PUBLISHED".equals(
                event.getMetadata().path("eventType").asText())).count()).isEqualTo(2);
    }

    @Test void identifiersAndMicroservicesUseIntersectionAndMissingScopeIsRejected() {
        String id = create("scope-key");
        String other = microservices.create(new MicroserviceFactory().valid("scope-other", "Other scope"))
                .expectCreated().extractBody().identifier();
        client.publishRequest(Map.of("identifiers", List.of(id))).expectBadRequest();
        client.publishRequest(Map.of("all", true)).expectBadRequest();
        client.publishRequest(Map.of("all", true, "microserviceIdentifiers", List.of())).expectBadRequest();
        client.publishRequest(Map.of("microserviceIdentifiers", List.of(parent))).expectBadRequest();
        client.publishRequest(Map.of("identifiers", List.of(id), "microserviceIdentifiers", List.of(other)))
                .expectAcceptedWithEmptyBody();
        // A subsequent synchronous bulk command also proves the AND restriction at the repository boundary.
        Long internalId = parameterKeyRepository.findRuntimeSnapshot(id).orElseThrow().getId();
        Long otherInternal = parameterKeyRepository.findRuntimeSnapshot(id).orElseThrow().getMicroserviceId() + 100000;
        assertThat(commands.publishBatch(List.of(internalId), List.of(otherInternal))).isEmpty();
        assertThat(version(id)).isZero();
        assertThat(value("scope-key")).isNull();
        // Omitted all is really false: only the supplied ID is published.
        String untouched = create("untouched-draft");
        client.publishRequest(Map.of("identifiers", List.of(id), "microserviceIdentifiers", List.of(parent)))
                .expectAcceptedWithEmptyBody();
        await(() -> value("scope-key") != null);
        assertThat(version(untouched)).isZero();
    }

    @Test void republishingPublishedKeysDoesNotUpdateMysqlAndStaleEditsConflict() {
        String id = create("republish-key");
        publish(id, "republish-key");
        client.update(id, factory.update(0, "Stale edit", "CONFIGURATION", "2")).expectConflict();
        redis.delete("account-service.republish-key");
        clearInvocations(parameterKeyRepository);
        publish(id, "republish-key");
        assertThat(version(id)).isEqualTo(1);
        verify(parameterKeyRepository, never()).publishDrafts(anyCollection(), anyCollection(), anyString(), anyString(), any());
    }

    @Test void redisRetryReusesTheSameSnapshotWithoutUpdatingMysqlAgainAndSchedulerRecovers() {
        String id = create("retry-key");
        AtomicInteger attempts = new AtomicInteger();
        doAnswer(invocation -> {
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            attempts.incrementAndGet();
            throw new ApiException("redis.unavailable");
        })
                .when(publicationStore).publish(anyMap());
        clearInvocations(parameterKeyRepository);
        client.publish(List.of(UUID.fromString(id)), List.of(UUID.fromString(parent))).expectAcceptedWithEmptyBody();
        await(() -> attempts.get() == 3);
        assertThat(version(id)).isEqualTo(1);
        assertThat(value("retry-key")).isNull();
        verify(parameterKeyRepository, times(1)).publishDrafts(anyCollection(), anyCollection(), anyString(), anyString(), any());
        org.mockito.ArgumentCaptor<Map<String, PublishedParameterValue>> batches = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(publicationStore, times(3)).publish(batches.capture());
        assertThat(batches.getAllValues().get(0)).isSameAs(batches.getAllValues().get(1)).isSameAs(batches.getAllValues().get(2));
        reset(publicationStore);
        publisher.reconcile();
        assertThat(value("retry-key")).isNotNull();
        assertThat(version(id)).isEqualTo(1);
    }

    @Test void changingOnlyTypeReturnsToDraftAndKeepsThePublishedTypeUntilManualPublication() {
        String expression = "{\"operator\":\"EQUALS\",\"valueType\":\"STRING\",\"value\":\"x\"}";
        String id = client.create(factory.valid(parent, "type-edit", "CONFIGURATION", expression)).expectCreated().identifier();
        publish(id, "type-edit");
        client.update(id, factory.update(1, "Changed type", "EXPRESSION", expression))
                .expectOk().expect("status", "DRAFT");
        assertThat(parameterKeyRepository.findRuntimeSnapshot(id).orElseThrow().getType()).isEqualTo("CONFIGURATION");
        assertThat(value("type-edit").path("type").asText()).isEqualTo("CONFIGURATION");
        client.publish(List.of(UUID.fromString(id)), List.of(UUID.fromString(parent))).expectAcceptedWithEmptyBody();
        await(() -> "EXPRESSION".equals(value("type-edit").path("type").asText()));
        assertThat(version(id)).isEqualTo(3);
    }

    @Test void bulkUpdateSkipsRowsThatBecameInactiveOrWereAlreadyPublished() {
        String inactive = create("changed-before-update");
        ParameterKeyRuntimeProjection selected = parameterKeyRepository.findRuntimeSnapshot(inactive).orElseThrow();
        client.inactivate(inactive).expectOk();
        long previous = version(inactive);
        assertThat(commands.publishBatch(List.of(selected.getId()), List.of(selected.getMicroserviceId()))).isEmpty();
        assertThat(version(inactive)).isEqualTo(previous);
        String published = create("already-published");
        publish(published, "already-published");
        ParameterKeyRuntimeProjection current = parameterKeyRepository.findRuntimeSnapshot(published).orElseThrow();
        assertThat(commands.publishBatch(List.of(current.getId()), List.of(current.getMicroserviceId()))).isEmpty();
        assertThat(version(published)).isEqualTo(1);
    }

    @Test void auditFailureRollsBackTheWholeBulkUpdateAndDoesNotPublishDraftValues() {
        String first = create("audit-rollback-a");
        String second = create("audit-rollback-b");
        ParameterKeyRuntimeProjection a = parameterKeyRepository.findRuntimeSnapshot(first).orElseThrow();
        ParameterKeyRuntimeProjection b = parameterKeyRepository.findRuntimeSnapshot(second).orElseThrow();
        doThrow(new IllegalStateException("simulated audit failure")).when(auditAppender).append(any(), any());
        try {
            assertThatThrownBy(() -> commands.publishBatch(List.of(a.getId(), b.getId()), List.of(a.getMicroserviceId())))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(version(first)).isZero();
            assertThat(version(second)).isZero();
            assertThat(parameterKeyRepository.findRuntimeSnapshot(first).orElseThrow().getValue()).isNull();
            assertThat(parameterKeyRepository.findRuntimeSnapshot(second).orElseThrow().getValue()).isNull();
            assertThat(value("audit-rollback-a")).isNull();
            assertThat(value("audit-rollback-b")).isNull();
        } finally { reset(auditAppender); }
    }

    @Test void bulkPublicationUsesOneUpdateAndProjectionQueriesRatherThanEntityLoadsPerKey() {
        String a = create("bulk-projection-a");
        String b = create("bulk-projection-b");
        ParameterKeyRuntimeProjection first = parameterKeyRepository.findRuntimeSnapshot(a).orElseThrow();
        ParameterKeyRuntimeProjection second = parameterKeyRepository.findRuntimeSnapshot(b).orElseThrow();
        clearInvocations(parameterKeyRepository);
        org.hibernate.stat.Statistics statistics = entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        boolean previousStatistics = statistics.isStatisticsEnabled();
        try {
            statistics.setStatisticsEnabled(true);
            statistics.clear();
            commands.publishBatch(List.of(first.getId(), second.getId()), List.of(first.getMicroserviceId()));
            assertThat(statistics.getEntityLoadCount()).isZero();
        } finally { statistics.setStatisticsEnabled(previousStatistics); }
        verify(parameterKeyRepository, times(1)).findDraftsForPublication(anyCollection(), anyCollection(), eq("DRAFT"));
        verify(parameterKeyRepository, times(1)).publishDrafts(anyCollection(), anyCollection(), eq("DRAFT"), eq("PUBLISHED"), any());
        verify(parameterKeyRepository, never()).save(any());
        verify(parameterKeyRepository, never()).saveAndFlush(any());
        assertThat(version(a)).isEqualTo(1);
        assertThat(version(b)).isEqualTo(1);
        assertThat(parameterKeyRepository.findRuntimeSnapshot(a).orElseThrow().getValue().path("value").asInt()).isEqualTo(1);
        assertThat(parameterKeyRepository.findRuntimeSnapshot(b).orElseThrow().getType()).isEqualTo("CONFIGURATION");
    }

    @Test void editConcurrentWithBulkPublicationRejectsTheVersionReadBeforePublication() throws Exception {
        String id = create("concurrent-edit");
        ParameterKeyRuntimeProjection selected = parameterKeyRepository.findRuntimeSnapshot(id).orElseThrow();
        java.util.concurrent.CountDownLatch locked = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        doAnswer(invocation -> {
            locked.countDown();
            if (!release.await(10, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("publication test barrier timed out");
            return invocation.callRealMethod();
        }).when(parameterKeyValidator).validatePublications(anyList());
        try (java.util.concurrent.ExecutorService executor = testUserExecutor(java.util.concurrent.Executors.newFixedThreadPool(2))) {
            java.util.concurrent.Future<?> publication = executor.submit(() -> commands.publishBatch(
                    List.of(selected.getId()), List.of(selected.getMicroserviceId())));
            assertThat(locked.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            java.util.concurrent.Future<?> edit = executor.submit(() -> client.update(id,
                    factory.update(0, "Concurrent edit", "CONFIGURATION", "2")).expectConflict());
            release.countDown();
            publication.get(10, java.util.concurrent.TimeUnit.SECONDS);
            edit.get(10, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(version(id)).isEqualTo(1);
            assertThat(client.findByIdentifier(id).expectOk().extractBody().status()).isEqualTo("PUBLISHED");
        } finally { release.countDown(); reset(parameterKeyValidator); }
    }

    @Test void reconciliationRemovesAnInactiveKeyRecreatedByAnOlderInFlightPublication() throws Exception {
        String id = create("inflight-convergence");
        java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch written = new java.util.concurrent.CountDownLatch(1);
        doAnswer(invocation -> {
            entered.countDown();
            if (!release.await(10, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Redis publication barrier timed out");
            try { return invocation.callRealMethod(); }
            finally { written.countDown(); }
        }).when(publicationStore).publish(anyMap());
        try {
            client.publish(List.of(UUID.fromString(id)), List.of(UUID.fromString(parent))).expectAcceptedWithEmptyBody();
            assertThat(entered.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            client.inactivate(id).expectOk();
            long inactiveVersion = version(id);
            assertThat(value("inflight-convergence")).isNull();
            release.countDown();
            assertThat(written.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(value("inflight-convergence")).isNotNull(); // accepted eventual-consistency window
            publisher.reconcile();
            assertThat(value("inflight-convergence")).isNull();
            assertThat(version(id)).isEqualTo(inactiveVersion);
            assertThat(client.findByIdentifier(id).expectOk().extractBody().active()).isFalse();
        } finally {
            release.countDown();
            reset(publicationStore);
            redis.delete("account-service.inflight-convergence");
        }
    }

    @Test void deletionRequiresInactiveAndMissingRedisKeyIsSuccessful() {
        String id = create("delete-absent");
        client.delete(id).expectBadRequest();
        client.inactivate(id).expectOk();
        client.inactivate(id).expectOk();
        client.delete(id).expectNoContent();
        client.findByIdentifier(id).expectNotFound();
    }

    @Test void realRedisCommunicationFailureBlocksDeletionButKeepsInactiveRowForReconciliation() {
        String id = create("delete-unavailable");
        publish(id, "delete-unavailable");
        LettuceConnectionFactory unavailable = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration("127.0.0.1", 1),
                LettuceClientConfiguration.builder().commandTimeout(Duration.ofMillis(100))
                        .shutdownTimeout(Duration.ZERO).build());
        unavailable.afterPropertiesSet();
        unavailable.start();
        try {
            PlatformRedisProperties properties = new Binder(new MapConfigurationPropertySource(Map.of(
                    "platform.redis.namespace", "dynamic-parameter", "platform.redis.namespace-mode", "SHARED")))
                    .bindOrCreate("platform.redis", PlatformRedisProperties.class);
            RedisStore failed = new DefaultRedisStore(new StringRedisTemplate(unavailable),
                    new RedisJsonCodec(json, 1048576), new RedisAccess(properties, "workspace-service"));
            doAnswer(invocation -> failed.deleteAll(invocation.<Collection<String>>getArgument(0)))
                    .when(publicationStore).removeAll(anyCollection());
            client.inactivate(id).expectOk();
            assertThat(value("delete-unavailable")).isNotNull();
            client.delete(id).expect5xx();
            assertThat(client.findByIdentifier(id).expectOk().extractBody().active()).isFalse();
            reset(publicationStore);
            publisher.reconcile();
            assertThat(value("delete-unavailable")).isNull();
            client.delete(id).expectNoContent();
        } finally { unavailable.destroy(); }
    }
}
