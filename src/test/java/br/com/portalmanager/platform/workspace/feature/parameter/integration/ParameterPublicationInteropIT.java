package br.com.portalmanager.platform.workspace.feature.parameter.integration;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.library.parameterclient.ConfigurationProvider;
import br.com.portalmanager.platform.library.parameterclient.ExpressionProvider;
import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.library.redis.internal.RedisAccess;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.feature.parameter.support.DynamicParameterRuntimeTestSupport;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.operations.ParameterKeyPublisherService;

import io.restassured.http.ContentType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ParameterPublicationInteropIT extends DynamicParameterRuntimeTestSupport {
    @Autowired private ConfigurationProvider configurations;
    @Autowired private ExpressionProvider expressions;
    @Autowired private RedisStore defaultStore;
    @Autowired private RedisAccess defaultAccess;
    @Autowired private StringRedisTemplate template;
    @Autowired private ParameterKeyPublisherService publisher;

    @Test
    void publishesBothKindsInOneBatchAndProvidersIgnoreTheDefaultStoreNamespace() {
        defaultStore.set("account-service.minimum-age", "default-value");
        String configuration = create("settings", "CONFIGURATION", "{\"retries\":3}");
        String expression = create("minimum-age", "EXPRESSION",
                "{\"operator\":\"GREATER_THAN_OR_EQUALS\",\"valueType\":\"INTEGER\",\"value\":18}");
        request().contentType(ContentType.JSON)
                .body(Map.of("identifiers", List.of(configuration, expression)))
                .post(BASE + "/publish").then().statusCode(202);
        await(() -> runtimeValue("settings") != null && runtimeValue("minimum-age") != null);

        assertThat(configurations.get("account-service.settings", Settings.class, null))
                .isEqualTo(new Settings(3));
        assertThat(expressions.matches("account-service.minimum-age", 21)).isTrue();
        assertThat(expressions.matches("account-service.minimum-age", 17)).isFalse();
        assertThat(defaultStore.get("account-service.minimum-age", String.class))
                .contains("default-value");
        assertThat(defaultAccess.prefix()).startsWith("test-").endsWith(".workspace-service:");
        assertThat(template.opsForValue().get(defaultAccess.prefix() + "account-service.minimum-age"))
                .isNotNull();
        assertThat(mapper.readTree(template.opsForValue().get("dynamic-parameter:account-service.minimum-age")))
                .isEqualTo(mapper.readTree("{\"type\":\"EXPRESSION\",\"value\":{\"operator\":\"GREATER_THAN_OR_EQUALS\",\"valueType\":\"INTEGER\",\"value\":18}}"));
        assertThat(redis.ttl("account-service.minimum-age")).isEmpty();
        assertThat(template.hasKey("config:account-service.minimum-age")).isFalse();
        assertThat(template.hasKey("dynamic-parameter:dynamic-parameter:account-service.minimum-age"))
                .isFalse();
    }

    @Test
    void updateKeepsThePublishedSnapshotUntilRepublishedThenReconciliationAndLifecycleRemainCompatible() {
        String identifier = create("threshold", "EXPRESSION",
                "{\"operator\":\"GREATER_THAN_OR_EQUALS\",\"valueType\":\"INTEGER\",\"value\":18}");
        publish(identifier);
        request().contentType(ContentType.JSON)
                .body(Map.of("version", 1, "type", "EXPRESSION", "value",
                        Map.of("operator", "GREATER_THAN_OR_EQUALS", "valueType", "INTEGER", "value", 25)))
                .put(BASE + "/" + identifier).then().statusCode(200);
        assertThat(expressions.matches("account-service.threshold", 21)).isTrue();
        publish(identifier);
        await(() -> !expressions.matches("account-service.threshold", 21));
        assertThat(expressions.matches("account-service.threshold", 21)).isFalse();
        redis.delete("account-service.threshold");
        assertThat(expressions.matches("account-service.threshold", 30, false)).isFalse();
        publisher.reconcile();
        assertThat(expressions.matches("account-service.threshold", 30)).isTrue();
        request().patch(BASE + "/" + identifier + "/inactivate").then().statusCode(200);
        assertThat(expressions.matches("account-service.threshold", 30, false)).isFalse();
        redis.set("account-service.threshold", Map.of("type", "CONFIGURATION", "value", 7));
        request().delete(BASE + "/" + identifier).then().statusCode(204);
        assertThat(configurations.get("account-service.threshold", 99)).isEqualTo(99);
    }

    public record Settings(int retries) {}
}
