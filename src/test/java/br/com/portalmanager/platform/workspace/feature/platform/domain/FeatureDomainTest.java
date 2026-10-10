package br.com.portalmanager.platform.workspace.feature.platform.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.workspace.feature.platform.domain.feature.Feature;
import br.com.portalmanager.platform.workspace.feature.platform.domain.microservice.Microservice;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class FeatureDomainTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 25, 13, 30);

    @Test
    void shouldMoveFeatureBetweenActiveServicesMaintainingBidirectionalRelation() {
        Microservice original = new Microservice("workspace-service", "Workspace Service", null, NOW);
        Microservice target = new Microservice("audit-service", "Audit Service", null, NOW);
        Feature feature = new Feature("audit", "audit", null, original, "{}", NOW);

        feature.changeMicroservice(target, NOW.plusMinutes(1));

        assertThat(original.getFeatures()).doesNotContain(feature);
        assertThat(target.getFeatures()).containsExactly(feature);
        assertThat(feature.getMicroservice()).isSameAs(target);
    }
}
