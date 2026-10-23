package br.com.portalmanager.platform.workspace.feature.parameter.support;

import static io.restassured.RestAssured.given;

import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.library.redis.annotation.RedisNamespace;
import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisProperties.NamespaceMode;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.lifecycle.PlatformAwait;
import br.com.portalmanager.platform.library.testing.redis.annotation.WithRedis;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Map;

public abstract class DynamicParameterRuntimeTestSupport extends br.com.portalmanager.platform.workspace.support.WorkspaceIntegrationTestSupport {
    protected static final String BASE = "/api/v1/dynamic-parameters";
    @LocalServerPort protected int port;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected ObjectMapper mapper;
    @Autowired
    @RedisNamespace(value = "dynamic-parameter", mode = NamespaceMode.SHARED)
    protected RedisStore redis;
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
        PlatformAwait.until(condition, Duration.ofSeconds(10), Duration.ofMillis(25));
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
