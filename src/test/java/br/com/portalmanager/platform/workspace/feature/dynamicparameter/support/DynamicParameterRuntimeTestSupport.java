package br.com.portalmanager.platform.workspace.feature.dynamicparameter.support;

import static io.restassured.RestAssured.given;

import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.redis.annotation.WithRedis;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@WithRedis
@org.springframework.test.context.TestPropertySource(
        properties = {
            "spring.data.redis.timeout=500ms",
            "spring.data.redis.connect-timeout=500ms",
            "platform.dynamic-parameter.runtime.enabled=true",
            "platform.dynamic-parameter.reconciliation.enabled=false",
            "platform.dynamic-parameter.runtime.batch-size=2",
            "platform.audit.enabled=true"
        })
public abstract class DynamicParameterRuntimeTestSupport {
    protected static final String BASE = "/api/v1/dynamic-parameters";
    @LocalServerPort protected int port;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected ObjectMapper mapper;
    @Autowired protected RedisStore redis;
    @Autowired protected AuthorizationMock authorization;
    protected String parent;

    @BeforeEach
    void prepareRuntime() {
        SchemaDefaultFixture.seed(jdbc);
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER").userName("parameter-owner"));
        parent =
                request()
                        .contentType(ContentType.JSON)
                        .body(
                                Map.of(
                                        "code",
                                        "account-service",
                                        "name",
                                        "Account",
                                        "settings",
                                        Map.of()))
                        .post("/api/v1/service-contexts/microservices")
                        .then()
                        .statusCode(201)
                        .extract()
                        .path("identifier");
    }

    @AfterEach
    void clearOwner() {
        UserContext.clear();
    }

    protected RequestSpecification request() {
        return given().port(port)
                .header("Authorization", "Bearer dynamic-parameter-runtime-review")
                .header("correlation-id", "dynamic-parameter-runtime-review")
                .accept(ContentType.JSON);
    }

    protected String create(String key, String type, String value) {
        return request()
                .contentType(ContentType.JSON)
                .body(
                        "{\"microserviceIdentifier\":\""
                                + parent
                                + "\",\"key\":\""
                                + key
                                + "\",\"type\":\""
                                + type
                                + "\",\"value\":"
                                + value
                                + "}")
                .post(BASE)
                .then()
                .statusCode(201)
                .extract()
                .path("identifier");
    }

    protected void publish(String identifier) {
        request()
                .contentType(ContentType.JSON)
                .body(Map.of("identifiers", java.util.List.of(identifier)))
                .post(BASE + "/publish")
                .then()
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
