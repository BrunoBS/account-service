package br.com.portalmanager.platform.workspace.integration.feature.parameter.support;

import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.library.redis.annotation.RedisNamespace;
import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisProperties.NamespaceMode;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.lifecycle.PlatformAwait;
import br.com.portalmanager.platform.library.testing.redis.annotation.WithRedis;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.foundation.audit.repository.WorkspaceAuditOutboxRepository;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.ParameterKeyRepository;
import br.com.portalmanager.platform.workspace.integration.support.SchemaDefaultScenario;


import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Map;

@WithRedis
public abstract class DynamicParameterRuntimeTestSupport extends br.com.portalmanager.platform.workspace.integration.support.WorkspaceIntegrationTestSupport {
    @Autowired protected PlatformRequestSpecificationFactory requests;
    private DynamicParameterClient client;
    private final DynamicParameterRequestFactory requestFactory = new DynamicParameterRequestFactory();
    @Autowired protected ParameterKeyRepository parameterKeys;
    @Autowired protected WorkspaceAuditOutboxRepository auditOutbox;
    @Autowired protected SchemaDefaultScenario schemaDefaults;
    @Autowired protected ObjectMapper mapper;
    @Autowired
    @RedisNamespace(value = "dynamic-parameter", mode = NamespaceMode.SHARED)
    protected RedisStore redis;
    @Autowired protected AuthorizationMock authorization;
    protected String parent;

    @BeforeEach
    void prepareRuntime() {
        client = new DynamicParameterClient(requests, JsonMapper.builder().build());
        parent = new DynamicParameterScenario(schemaDefaults, authorization,
                new MicroserviceClient(requests, JsonMapper.builder().build())).setup();
    }

    protected String create(String key, String type, String value) {
        return client.create(requestFactory.valid(parent, key, type, value))
                .expectCreated().identifier();
    }

    protected void publish(String identifier) {
        client.publish(identifier).expectAcceptedWithEmptyBody();
        String key = parameterKeys.findRuntimeSnapshot(identifier).orElseThrow().getKey();
        await(() -> runtimeValue(key) != null);
    }

    protected DynamicParameterResponse publishBatch(Object identifiers) {
        return client.publishBatch(identifiers);
    }

    protected DynamicParameterResponse updateParameter(String identifier, Object body) {
        return client.update(identifier, body);
    }

    protected DynamicParameterResponse inactivateParameter(String identifier) {
        return client.inactivate(identifier);
    }

    protected DynamicParameterResponse activateParameter(String identifier) {
        return client.activate(identifier);
    }

    protected DynamicParameterResponse deleteParameter(String identifier) {
        return client.delete(identifier);
    }

    protected DynamicParameterResponse publishRemovedSingleEndpoint(String identifier) {
        return client.publishLegacy(identifier);
    }

    protected DynamicParameterResponse publishRemovedBatchEndpoint(Object identifiers) {
        return client.publishLegacyBatch(identifiers);
    }

    protected void await(java.util.function.BooleanSupplier condition) {
        PlatformAwait.until(condition, Duration.ofSeconds(10), Duration.ofMillis(25));
    }

    protected JsonNode runtimeValue(String key) {
        return redis.get("account-service." + key, JsonNode.class).orElse(null);
    }

    protected int publishedAuditCount() {
        return (int) auditOutbox.findAll().stream()
                .filter(entry -> entry.getMetadata().path("eventType").asText()
                        .equals("PARAMETER_KEY_PUBLISHED"))
                .count();
    }

    protected int publishedAuditCount(String username) {
        return (int) auditOutbox.findAll().stream()
                .filter(entry -> entry.getMetadata().path("eventType").asText()
                        .equals("PARAMETER_KEY_PUBLISHED"))
                .filter(entry -> entry.getMetadata().path("username").asText().equals(username))
                .count();
    }
}
