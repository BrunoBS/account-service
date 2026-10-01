package br.com.portalmanager.platform.workspace.feature.platform.domain;

import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MicroserviceDomainTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 25, 13, 45);

    @Test
    void shouldTransitionLifecycleAndUpdateTimestamp() {
        Microservice microservice = new Microservice("audit-service", "Audit Service", null, JSON.createObjectNode(), NOW);
        assertThat(microservice.isActive()).isTrue();
        assertThat(microservice.getLifecycle()).isEqualTo(LifecycleTypeCode.active());
        microservice.inactivate(NOW.plusMinutes(1));
        assertThat(microservice.isActive()).isFalse();
        microservice.activate(NOW.plusMinutes(2));
        assertThat(microservice.isActive()).isTrue();
        microservice.quarantine(NOW.plusMinutes(3));
        assertThat(microservice.isActive()).isFalse();
        assertThat(microservice.getLifecycle()).isEqualTo(LifecycleTypeCode.quarantined());
    }

    @Test
    void shouldExposeFeaturesAsReadOnlyRelation() {
        Microservice microservice = new Microservice("audit-service", "Audit Service", null, JSON.createObjectNode(), NOW);
        Feature feature = new Feature("audit", "Audit", null, microservice, JSON.createObjectNode(), NOW);
        assertThat(microservice.getFeatures()).containsExactly(feature);
    }
}
