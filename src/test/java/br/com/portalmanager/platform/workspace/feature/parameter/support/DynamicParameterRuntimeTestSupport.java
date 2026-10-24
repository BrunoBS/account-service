package br.com.portalmanager.platform.workspace.feature.parameter.support;

import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.library.redis.annotation.RedisNamespace;
import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisProperties.NamespaceMode;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.redis.annotation.WithRedis;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

@WithRedis
@org.springframework.test.context.TestPropertySource(
        properties = {
            "spring.data.redis.timeout=500ms",
            "spring.data.redis.connect-timeout=500ms",
            "workspace.parameter.runtime.enabled=true",
            "workspace.parameter.reconciliation.enabled=false",
            "workspace.parameter.runtime.batch-size=2",
            "platform.audit.enabled=true"
        })
public abstract class DynamicParameterRuntimeTestSupport {
    protected static final String BASE = "/api/v1/dynamic-parameters";
    @Autowired protected PlatformRequestSpecificationFactory requests;
    private DynamicParameterClient client;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected ObjectMapper mapper;
    @Autowired
    @RedisNamespace(value = "dynamic-parameter", mode = NamespaceMode.SHARED)
    protected RedisStore redis;
    @Autowired protected AuthorizationMock authorization;
    protected String parent;

    @BeforeEach
    void prepareRuntime() {
        client = new DynamicParameterClient(requests, JsonMapper.builder().build());
        SchemaDefaultFixture.seed(jdbc);
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER").userName("parameter-owner"));
        parent = new MicroserviceClient(requests, JsonMapper.builder().build())
                .createParent().expectCreated().identifier();
    }

    @AfterEach
    void clearOwner() {
        UserContext.clear();
    }

    protected RequestSpecification request() {
        return client.protocolRequest();
    }

    protected String create(String key, String type, String value) {
        return client.createRaw(DynamicParameterRequestFactory.create(parent, key, type, value))
                .expectCreated().identifier();
    }

    protected void publish(String identifier) {
        client.publish(identifier).response()
                .statusCode(202)
                .body(org.hamcrest.Matchers.emptyString());
        await(() -> runtimeValue(parameterKey(identifier)) != null);
    }

    protected String parameterKey(String identifier) {
        return jdbc.queryForObject(
                "SELECT parameter_key FROM parameter_keys WHERE identifier=?",
                String.class,
                identifier);
    }

    protected void await(java.util.function.BooleanSupplier condition) {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) return;
            try {
                Thread.sleep(25);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError(exception);
            }
        }
        org.assertj.core.api.Assertions.assertThat(condition.getAsBoolean()).isTrue();
    }

    protected JsonNode runtimeValue(String key) {
        return redis.get("account-service." + key, JsonNode.class).orElse(null);
    }

    protected int publishedAuditCount() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_outbox WHERE"
                    + " JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.eventType'))='PARAMETER_KEY_PUBLISHED'",
                Integer.class);
    }
}
