package br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.support.DynamicParameterRuntimeTestSupport;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.operations.runtime.DynamicParameterRuntimeService;

import io.restassured.http.ContentType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ParameterKeyReconciliationIT extends DynamicParameterRuntimeTestSupport {
    @Autowired private DynamicParameterRuntimeService runtime;

    @Test
    void restoresPublishedSnapshotsAcrossPagesWithoutScanningOrRemovingOtherRedisKeys() {
        for (int index = 0; index < 5; index++) {
            String identifier =
                    create("published-" + index, "CONFIGURATION", Integer.toString(index));
            publish(identifier);
            redis.delete("account-service.published-" + index);
        }
        create("draft", "CONFIGURATION", "9");
        String inactive = create("inactive", "CONFIGURATION", "10");
        publish(inactive);
        request().patch(BASE + "/" + inactive + "/inactivate").then().statusCode(200);
        redis.set("unmanaged.orphan", "preserved");
        runtime.reconcile();
        for (int index = 0; index < 5; index++)
            assertThat(runtimeValue("published-" + index).get("value").intValue()).isEqualTo(index);
        assertThat(runtimeValue("draft")).isNull();
        assertThat(runtimeValue("inactive")).isNull();
        assertThat(redis.get("unmanaged.orphan", String.class)).contains("preserved");
    }

    @Test
    void hourlySynchronizationUsesPublishedTypeAndValueEvenWhenCurrentContentIsDraft() {
        String identifier = create("snapshot", "CONFIGURATION", "{\"approved\":true}");
        publish(identifier);
        request()
                .contentType(ContentType.JSON)
                .body(
                        Map.of(
                                "version",
                                1,
                                "type",
                                "EXPRESSION",
                                "value",
                                Map.of("operator", "EQUALS", "valueType", "LONG", "value", 10)))
                .put(BASE + "/" + identifier)
                .then()
                .statusCode(200);
        redis.delete("account-service.snapshot");
        runtime.reconcile();
        assertThat(runtimeValue("snapshot"))
                .isEqualTo(
                        mapper.readTree(
                                "{\"type\":\"CONFIGURATION\",\"value\":{\"approved\":true}}"));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT status_code FROM parameter_keys WHERE identifier=?",
                                String.class,
                                identifier))
                .isEqualTo("DRAFT");
    }

    @Test
    void inactivationAndDeletionRemoveImmediatelyAndReactivationWaitsForPublication() {
        String identifier = create("lifecycle", "CONFIGURATION", "1");
        publish(identifier);
        request().patch(BASE + "/" + identifier + "/inactivate").then().statusCode(200);
        assertThat(runtimeValue("lifecycle")).isNull();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT active=false AND status_code='DRAFT' AND published_value IS"
                                        + " NULL FROM parameter_keys WHERE identifier=?",
                                Boolean.class,
                                identifier))
                .isTrue();
        request().patch(BASE + "/" + identifier + "/activate").then().statusCode(200);
        runtime.reconcile();
        assertThat(runtimeValue("lifecycle")).isNull();
        publish(identifier);
        request().patch(BASE + "/" + identifier + "/inactivate").then().statusCode(200);
        redis.set("account-service.lifecycle", "residual");
        request().delete(BASE + "/" + identifier).then().statusCode(204);
        assertThat(runtimeValue("lifecycle")).isNull();
    }
}
