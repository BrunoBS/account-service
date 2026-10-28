package br.com.portalmanager.platform.workspace.integration.feature.parameter.key;

import br.com.portalmanager.platform.library.redis.annotation.RedisNamespace;
import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisProperties.NamespaceMode;
import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.lifecycle.PlatformAwait;
import br.com.portalmanager.platform.library.testing.redis.annotation.WithRedis;
import br.com.portalmanager.platform.workspace.entrypoint.parameter.response.ParameterCacheResponse;
import br.com.portalmanager.platform.workspace.entrypoint.parameter.response.ParameterKeyResponse;
import br.com.portalmanager.platform.workspace.integration.feature.parameter.support.DynamicParameterClient;
import br.com.portalmanager.platform.workspace.integration.feature.parameter.support.DynamicParameterRequestFactory;
import br.com.portalmanager.platform.workspace.integration.feature.parameter.support.DynamicParameterScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.operations.ParameterKeyPublisherService;
import br.com.portalmanager.platform.workspace.foundation.audit.repository.WorkspaceAuditOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@WithRedis
class ParameterCacheIntegrationTest extends BaseIntegrationTest {
    @Autowired private DynamicParameterClient client;
    @Autowired private DynamicParameterScenario scenario;
    @Autowired private AuthorizationMock authorization;
    @Autowired private ParameterKeyPublisherService publisher;
    @Autowired private WorkspaceAuditOutboxRepository auditOutbox;
    @Autowired private StringRedisTemplate raw;
    @Autowired @Qualifier("applicationTaskExecutor") private TaskExecutor executor;
    @Autowired @RedisNamespace(value = "dynamic-parameter", mode = NamespaceMode.SHARED) private RedisStore redis;
    private final DynamicParameterRequestFactory factory = new DynamicParameterRequestFactory();
    private String parent;
    private String code;
    private final Set<String> ownedKeys = new LinkedHashSet<>();
    private final Set<String> rawKeys = new LinkedHashSet<>();
    @Autowired private br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceClient microservices;

    @BeforeEach void prepare() {
        scenario.setup();
        code = "cache-" + UUID.randomUUID().toString().replace("-", "");
        parent = microservices.create(new br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceFactory()
                .valid(code, "Cache test " + UUID.randomUUID())).expectCreated().extractBody().identifier();
    }

    private void await(java.util.function.BooleanSupplier condition) {
        PlatformAwait.until(condition, Duration.ofSeconds(10), Duration.ofMillis(25));
    }
    private void awaitIdle() {
        if (executor instanceof ThreadPoolTaskExecutor pool) await(() -> pool.getActiveCount() == 0 && pool.getThreadPoolExecutor().getQueue().isEmpty());
    }
    private String create(String key) {
        ownedKeys.add(key(key));
        return client.create(factory.valid(parent, key, "CONFIGURATION", "{\"secret\":\"not-in-listing\"}"))
                .expectCreated().identifier();
    }
    private void publish(String id, String key) {
        client.publish(List.of(UUID.fromString(id)), List.of(UUID.fromString(parent))).expectAcceptedWithEmptyBody();
        await(() -> redis.exists(key("") + key));
        awaitIdle();
    }

    private String key(String suffix) { return code + "." + suffix; }
    private void putCache(String key, String value) {
        ownedKeys.add(key);
        redis.set(key, value);
    }
    private void putRaw(String key, String value) {
        rawKeys.add(key);
        raw.opsForValue().set(key, value);
    }
    @AfterEach void cleanOwnedCacheKeys() {
        awaitIdle();
        if (!ownedKeys.isEmpty()) redis.deleteAll(ownedKeys);
        if (!rawKeys.isEmpty()) raw.delete(rawKeys);
    }

    @Test void cursorListsAllNamesOfOneMicroserviceIncludingOrphansWithoutReadingValues() {
        Set<String> expected = new LinkedHashSet<>();
        for (int index = 0; index < 19; index++) {
            String key = key("orphan-") + index;
            expected.add(key);
            putRaw("dynamic-parameter:" + key, "invalid-json-secret-value");
        }
        putCache(code + "-other.hidden", "foreign-microservice");
        putRaw("foreign:" + key("hidden"), "foreign-namespace");
        Set<String> found = new LinkedHashSet<>();
        String cursor = "0";
        int iterations = 0;
        do {
            ParameterCacheResponse page = client.cache(parent, cursor, 1).expectOk().extractCache();
            assertThat(page.complete()).isEqualTo("0".equals(page.cursor()));
            assertThat(page.keys()).allMatch(key -> key.startsWith(key("")));
            found.addAll(page.keys()); cursor = page.cursor();
            assertThat(++iterations).isLessThan(10000);
        } while (!"0".equals(cursor));
        assertThat(found).containsExactlyInAnyOrderElementsOf(expected);
        assertThat(iterations).isGreaterThan(1);
    }

    @Test void clearReturns202BeforeWorkFinishesAndRemovesOrphansWithoutChangingMysql() throws Exception {
        String published = create("published-cache");
        publish(published, "published-cache");
        String draft = create("draft-cache");
        putCache(key("orphan"), "orphan");
        putCache(code + "-other.foreign", "preserve");
        putRaw("foreign:" + key("orphan"), "preserve");
        ParameterKeyResponse before = client.findByIdentifier(published).expectOk().extractBody();
        long rows = parameterKeyRepository.count();
        long audits = auditOutbox.count();
        clearInvocations(parameterKeyRepository);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            entered.countDown();
            if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("cache test barrier timed out");
            return invocation.callRealMethod();
        }).when(publicationStore).scan(eq(code), anyString(), anyInt());
        try {
            client.clearCache(parent).expectAcceptedWithEmptyBody();
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(redis.exists(key("orphan"))).isTrue();
        } finally { release.countDown(); }
        await(() -> !redis.exists(key("orphan")) && !redis.exists(key("published-cache")));
        awaitIdle();
        assertThat(redis.exists(code + "-other.foreign")).isTrue();
        assertThat(raw.hasKey("foreign:" + key("orphan"))).isTrue();
        assertThat(parameterKeyRepository.count()).isEqualTo(rows);
        assertThat(auditOutbox.count()).isEqualTo(audits);
        assertThat(client.findByIdentifier(published).expectOk().extractBody()).isEqualTo(before);
        assertThat(client.findByIdentifier(draft).expectOk().extractBody().status()).isEqualTo("DRAFT");
        assertThat(client.findByIdentifier(draft).expectOk().extractBody().version()).isZero();
        verify(parameterKeyRepository, never()).publishDrafts(anyCollection(), anyCollection(), anyString(), anyString(), any());
    }

    @Test void clearIsIdempotentForAnEmptyCache() {
        client.clearCache(parent).expectAcceptedWithEmptyBody();
        awaitIdle();
        client.clearCache(parent).expectAcceptedWithEmptyBody();
        awaitIdle();
        String cursor = "0";
        do {
            ParameterCacheResponse page = client.cache(parent, cursor, null).expectOk().extractCache();
            assertThat(page.keys()).isEmpty();
            cursor = page.cursor();
        } while (!"0".equals(cursor));
    }

    @Test void requestsRequireAnExistingMicroserviceAndValidCursorAndCount() {
        client.cache(null, null, null).expectBadRequest();
        client.clearCache(null).expectBadRequest();
        client.cache("not-a-uuid", null, null).expectBadRequest();
        client.clearCache("not-a-uuid").expectBadRequest();
        String missing = UUID.randomUUID().toString();
        client.cache(missing, null, null).expectNotFound();
        client.clearCache(missing).expectNotFound();
        for (String cursor : List.of("abc", "-1", "18446744073709551616")) client.cache(parent, cursor, 1).expectBadRequest();
        client.cache(parent, "0", 0).expectBadRequest();
        client.cache(parent, "0", 3).expectBadRequest(); // configured batchSize=2
        verify(publicationStore, never()).removeAll(anyCollection());
    }

    @Test void ownerPolicyDenialPreventsListingAndClearingCache() {
        putCache(key("protected"), "preserve");
        authorization.reset();
        authorization.allow(session -> session.userName("reader").groups("PM5_READER"));
        authorization.forbiddenResource(resource -> resource.header("policy", "OWNER"));
        client.cache(parent, null, null).expectForbidden();
        client.clearCache(parent).expectForbidden();
        verify(publicationStore, never()).scan(anyString(), anyString(), anyInt());
        verify(publicationStore, never()).removeAll(anyCollection());
        assertThat(redis.exists(key("protected"))).isTrue();
        authorization.verifyCalledWithPolicy("OWNER");
    }

    @Test void authorizationProviderDenialDoesNotReachTheCache() {
        authorization.reset(); authorization.forbidden();
        client.cache(parent, null, null).expectForbidden();
        client.clearCache(parent).expectForbidden();
        verifyNoInteractions(publicationStore);
    }

    @Test void realRedisConnectionFailureIsNotReportedAsAnEmptyPageOrSuccessfulClear() {
        org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory unavailable =
                new org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory(
                        new org.springframework.data.redis.connection.RedisStandaloneConfiguration("127.0.0.1", 1),
                        org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration.builder()
                                .commandTimeout(Duration.ofMillis(100)).shutdownTimeout(Duration.ZERO).build());
        unavailable.afterPropertiesSet(); unavailable.start();
        try {
            br.com.portalmanager.platform.library.redis.configuration.PlatformRedisProperties properties =
                    new org.springframework.boot.context.properties.bind.Binder(
                            new org.springframework.boot.context.properties.source.MapConfigurationPropertySource(java.util.Map.of(
                                    "platform.redis.namespace", "dynamic-parameter", "platform.redis.namespace-mode", "SHARED")))
                            .bindOrCreate("platform.redis", br.com.portalmanager.platform.library.redis.configuration.PlatformRedisProperties.class);
            RedisStore failed = new br.com.portalmanager.platform.library.redis.internal.DefaultRedisStore(
                    new StringRedisTemplate(unavailable),
                    new br.com.portalmanager.platform.library.redis.serialization.RedisJsonCodec(new tools.jackson.databind.json.JsonMapper(), 1048576),
                    new br.com.portalmanager.platform.library.redis.internal.RedisAccess(properties, "workspace-service"));
            doAnswer(invocation -> {
                br.com.portalmanager.platform.library.redis.core.RedisKeyPage page = failed.scan(
                        invocation.<String>getArgument(0) + ".", invocation.getArgument(1), invocation.getArgument(2));
                return new br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.ParameterCachePage(page.cursor(), page.keys());
            }).when(publicationStore).scan(anyString(), anyString(), anyInt());
            client.cache(parent, null, null).expect5xx();
            client.clearCache(parent).expectAcceptedWithEmptyBody();
            awaitIdle();
            verify(publicationStore, times(4)).scan(eq(code), eq("0"), eq(2));
            verify(publicationStore, never()).removeAll(anyCollection());
        } finally { unavailable.destroy(); reset(publicationStore); }
    }

    @Test void schedulerRepopulatesPublishedSnapshotsAfterExplicitClearWithoutPromotingDrafts() {
        String published = create("repopulate");
        publish(published, "repopulate");
        String draft = create("not-published");
        client.clearCache(parent).expectAcceptedWithEmptyBody();
        await(() -> !redis.exists(key("repopulate")));
        awaitIdle();
        publisher.reconcile();
        assertThat(redis.exists(key("repopulate"))).isTrue();
        assertThat(redis.exists(key("not-published"))).isFalse();
        assertThat(client.findByIdentifier(published).expectOk().extractBody().version()).isEqualTo(1);
        assertThat(client.findByIdentifier(draft).expectOk().extractBody().version()).isZero();
    }
}
