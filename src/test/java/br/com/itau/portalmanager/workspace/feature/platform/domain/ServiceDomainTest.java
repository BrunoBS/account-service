package br.com.itau.portalmanager.workspace.feature.platform.domain;

import br.com.itau.portalmanager.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceDomainTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 25, 13, 45);

    @Test
    void shouldTransitionLifecycleAndUpdateTimestamp() {
        Service service = new Service("audit-service", "Audit Service", null, NOW);

        assertThat(service.isActive()).isTrue();
        assertThat(service.getLifecycle()).isEqualTo(LifecycleTypeCode.active());

        service.inactivate(NOW.plusMinutes(1));
        assertThat(service.isActive()).isFalse();
        assertThat(service.getLifecycle()).isEqualTo(LifecycleTypeCode.inactive());
        assertThat(service.getUpdatedAt()).isEqualTo(NOW.plusMinutes(1));

        service.activate(NOW.plusMinutes(2));
        assertThat(service.isActive()).isTrue();
        assertThat(service.getLifecycle()).isEqualTo(LifecycleTypeCode.active());

        service.quarantine(NOW.plusMinutes(3));
        assertThat(service.isActive()).isFalse();
        assertThat(service.getLifecycle()).isEqualTo(LifecycleTypeCode.quarantined());
        assertThat(service.getUpdatedAt()).isEqualTo(NOW.plusMinutes(3));
    }

    @Test
    void shouldExposeFeaturesAsReadOnlyRelation() {
        Service service = new Service("audit-service", "Audit Service", null, NOW);
        Feature feature = new Feature("AUDIT", "Audit", null, service, "{}", NOW);

        assertThat(service.getFeatures()).containsExactly(feature);
    }
}
