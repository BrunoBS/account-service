package br.com.portalmanager.platform.workspace.feature.parameter.usecase;

import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.feature.parameter.support.DynamicParameterRuntimeTestSupport;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.operations.ParameterKeyPublisherService;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("Integração HTTP - Parameter Key Reconciliation")
class ParameterKeyReconciliationIT extends DynamicParameterRuntimeTestSupport {
    @Autowired private ParameterKeyPublisherService publisher;

    @Test

    @DisplayName("RESTORE - restores published snapshots across pages without scanning or removing other redis keys")
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
        inactivateParameter(inactive).expectOk();
        redis.set("unmanaged.orphan", "preserved");
        publisher.reconcile();
        for (int index = 0; index < 5; index++)
            assertThat(runtimeValue("published-" + index).get("value").intValue()).isEqualTo(index);
        assertThat(runtimeValue("draft")).isNull();
        assertThat(runtimeValue("inactive")).isNull();
        assertThat(redis.get("unmanaged.orphan", String.class)).contains("preserved");
    }

    @Test

    @DisplayName("CREATE - hourly synchronization uses published type and value even when current content is draft")
    void hourlySynchronizationUsesPublishedTypeAndValueEvenWhenCurrentContentIsDraft() {
        String identifier = create("snapshot", "CONFIGURATION", "{\"approved\":true}");
        publish(identifier);
        updateParameter(identifier,
                Map.of("version", 1, "type", "EXPRESSION", "value",
                        Map.of("operator", "EQUALS", "valueType", "LONG", "value", 10)))
                .expectOk();
        redis.delete("account-service.snapshot");
        publisher.reconcile();
        assertThat(runtimeValue("snapshot"))
                .isEqualTo(
                        mapper.readTree(
                                "{\"type\":\"CONFIGURATION\",\"value\":{\"approved\":true}}"));
        assertThat(parameterKeys.findProjection(identifier).orElseThrow().getStatus())
                .isEqualTo("DRAFT");
    }

    @Test

    @DisplayName("RESTORE - inactivation and deletion remove immediately and reactivation waits for publication")
    void inactivationAndDeletionRemoveImmediatelyAndReactivationWaitsForPublication() {
        String identifier = create("lifecycle", "CONFIGURATION", "1");
        publish(identifier);
        inactivateParameter(identifier).expectOk();
        assertThat(runtimeValue("lifecycle")).isNull();
        var inactivated = parameterKeys.findRuntimeSnapshot(identifier).orElseThrow();
        var metadata = parameterKeys.findProjection(identifier).orElseThrow();
        assertThat(inactivated.isActive()).isFalse();
        assertThat(inactivated.getValue()).isNull();
        assertThat(metadata.getStatus()).isEqualTo("DRAFT");
        activateParameter(identifier).expectOk();
        publisher.reconcile();
        assertThat(runtimeValue("lifecycle")).isNull();
        publish(identifier);
        inactivateParameter(identifier).expectOk();
        redis.set("account-service.lifecycle", "residual");
        deleteParameter(identifier).expectNoContent();
        assertThat(runtimeValue("lifecycle")).isNull();
    }
}
