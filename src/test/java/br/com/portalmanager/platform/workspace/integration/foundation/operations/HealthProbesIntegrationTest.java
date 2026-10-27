package br.com.portalmanager.platform.workspace.integration.foundation.operations;

import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.library.testing.redis.annotation.WithRedis;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.aMapWithSize;

@WithRedis
class HealthProbesIntegrationTest extends BaseIntegrationTest {
    @Autowired private PlatformRequestSpecificationFactory requests;
    @Autowired private HealthContributorRegistry health;
    @Autowired private org.springframework.boot.health.registry.ReactiveHealthContributorRegistry reactiveHealth;

    @Test void exposesOnlyStatusWithoutCredentialsOrInternalDetails() {
        requests.create().get("/actuator/health/liveness").then().statusCode(200).body("status", equalTo("UP")).body("", aMapWithSize(1));
        requests.create().get("/actuator/health/readiness").then().statusCode(200).body("status", equalTo("UP")).body("", aMapWithSize(1));
        requests.create().get("/actuator/env").then().statusCode(404);
        requests.create().get("/actuator/configprops").then().statusCode(404);
    }

    @Test void databaseOutageRefusesTrafficWithoutFailingLiveness() { assertDependencyFailure("db"); }
    @Test void redisOutageRefusesTrafficWithoutFailingLiveness() {
        org.springframework.boot.health.contributor.ReactiveHealthContributor original = reactiveHealth.unregisterContributor("redis");
        org.assertj.core.api.Assertions.assertThat(original).isNotNull();
        try {
            reactiveHealth.registerContributor("redis", (org.springframework.boot.health.contributor.ReactiveHealthIndicator) () -> reactor.core.publisher.Mono.just(Health.down().build()));
            requests.create().get("/actuator/health/readiness").then().statusCode(503).body("status", equalTo("DOWN")).body("", aMapWithSize(1));
            requests.create().get("/actuator/health/liveness").then().statusCode(200).body("status", equalTo("UP"));
        } finally {
            reactiveHealth.unregisterContributor("redis");
            reactiveHealth.registerContributor("redis", original);
        }
        requests.create().get("/actuator/health/readiness").then().statusCode(200);
    }

    private void assertDependencyFailure(String name) {
        HealthContributor original = health.unregisterContributor(name);
        org.assertj.core.api.Assertions.assertThat(original).isNotNull();
        try {
            health.registerContributor(name, (HealthIndicator) () -> Health.down().withDetail("secret", "must-not-be-exposed").build());
            requests.create().get("/actuator/health/readiness").then().statusCode(503).body("status", equalTo("DOWN")).body("", aMapWithSize(1));
            requests.create().get("/actuator/health/liveness").then().statusCode(200).body("status", equalTo("UP"));
        } finally {
            health.unregisterContributor(name);
            health.registerContributor(name, original);
        }
        requests.create().get("/actuator/health/readiness").then().statusCode(200);
    }
}
