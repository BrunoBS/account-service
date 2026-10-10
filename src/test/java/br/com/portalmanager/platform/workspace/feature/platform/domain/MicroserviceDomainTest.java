package br.com.portalmanager.platform.workspace.feature.platform.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.workspace.feature.platform.domain.feature.Feature;
import br.com.portalmanager.platform.workspace.feature.platform.domain.microservice.Microservice;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class MicroserviceDomainTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 25, 13, 45);

    @Test
    void shouldTransitionLifecycleAndUpdateTimestamp() {
        Microservice microservice = new Microservice("audit-service", "Audit Service", null, "{}", NOW);
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
        Microservice microservice = new Microservice("audit-service", "Audit Service", null, "{}", NOW);
        Feature feature = new Feature("audit", "Audit", null, microservice, "{}", NOW);
        assertThat(microservice.getFeatures()).containsExactly(feature);
    }
}
